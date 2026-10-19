package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.dto.request.RegistrarProgressoRequest;
import com.readrace.api.dto.response.ProgressoLeituraResponse;
import com.readrace.api.model.SequenciaDeLeitura;
import com.readrace.api.repository.MembroClubeRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistrarProgressoConcorrenciaIT {
    private static final UUID LIVRO = UUID.fromString("30000000-0000-0000-0000-000000000013");
    private static final UUID USUARIO = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DESAFIO_ATIVO =
            UUID.fromString("90000000-0000-0000-0000-000000000001");
    private static final UUID CLUBE = UUID.fromString("59000000-0000-0000-0000-000000000002");

    @Autowired private RegistrarProgressoService service;
    @Autowired private MembroClubeRepository membroClubeRepository;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    private int placarAntes;
    private int xpUsuarioAntes;
    private int nivelUsuarioAntes;
    private int sequenciaAntes;
    private LocalDate ultimaLeituraAntes;

    @BeforeEach
    void guardar_estado_do_usuario() {
        placarAntes = placarDoDesafioAtivo();
        xpUsuarioAntes = xpDoUsuario();
        nivelUsuarioAntes = nivelDoUsuario();
        sequenciaAntes = sequencia();
        ultimaLeituraAntes = ultimaLeitura();

        // Parte de um estado conhecido: leu ontem e está com sequência 12.
        jdbc.update(
                "UPDATE usuario SET dias_consecutivos = 12, ultima_leitura_em = ? WHERE id = ?",
                SequenciaDeLeitura.hoje().minusDays(1),
                USUARIO);
    }

    @BeforeEach
    void criar_clube_que_le_o_livro() {
        // O livro do teste não é o livro atual de nenhum clube do seed: este clube existe só aqui,
        // com o usuário como membro e zero pontos.
        jdbc.update(
                "INSERT INTO clube_do_livro (id, livro_id, nome) VALUES (?, ?, 'Clube do teste')",
                CLUBE,
                LIVRO);
        jdbc.update(
                "INSERT INTO membro_clube (id, clube_id, usuario_id, pontos, cargo_clube)"
                        + " VALUES (?, ?, ?, 0, 'membro'::cargo)",
                UUID.randomUUID(),
                CLUBE,
                USUARIO);
    }

    @AfterEach
    void limpar_registros_confirmados() {
        // Os registros também avançam o desafio ativo, o XP e a sequência do usuário e, aqui, são
        // confirmados de verdade — sem isso, os testes seguintes herdariam o estado sujo.
        jdbc.update(
                "UPDATE progresso_desafio SET valor_atual = ?"
                        + " WHERE desafio_id = ? AND usuario_id = ?",
                placarAntes,
                DESAFIO_ATIVO,
                USUARIO);
        jdbc.update(
                "UPDATE usuario SET xp_total = ?, nivel = ?, dias_consecutivos = ?,"
                        + " ultima_leitura_em = ? WHERE id = ?",
                xpUsuarioAntes,
                nivelUsuarioAntes,
                sequenciaAntes,
                ultimaLeituraAntes,
                USUARIO);
        jdbc.update(
                """
                DELETE FROM registro_leitura WHERE item_biblioteca_id IN
                (SELECT id FROM item_biblioteca WHERE usuario_id = ? AND livro_id = ?)
                """,
                USUARIO,
                LIVRO);
        jdbc.update(
                "DELETE FROM item_biblioteca WHERE usuario_id = ? AND livro_id = ?",
                USUARIO,
                LIVRO);
        jdbc.update("DELETE FROM membro_clube WHERE clube_id = ?", CLUBE);
        jdbc.update("DELETE FROM clube_do_livro WHERE id = ?", CLUBE);
    }

    @Test
    void deve_criar_um_item_e_pagar_cada_pagina_uma_vez_em_transacoes_concorrentes()
            throws Exception {
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM item_biblioteca WHERE usuario_id = ? AND livro_id = ?",
                                Integer.class,
                                USUARIO,
                                LIVRO))
                .isZero();
        verificarChamadasConcorrentes(10, 10);
        verificarChamadasConcorrentes(20, 10);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM item_biblioteca WHERE usuario_id = ? AND livro_id = ?",
                                Integer.class,
                                USUARIO,
                                LIVRO))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT pagina_maxima FROM item_biblioteca WHERE usuario_id = ? AND livro_id = ?",
                                Integer.class,
                                USUARIO,
                                LIVRO))
                .isEqualTo(20);
        assertThat(
                        jdbc.queryForObject(
                                """
                SELECT count(*) FROM registro_leitura r JOIN item_biblioteca i ON i.id = r.item_biblioteca_id
                WHERE i.usuario_id = ? AND i.livro_id = ?
                """,
                                Integer.class,
                                USUARIO,
                                LIVRO))
                .isEqualTo(24);
        assertThat(placarDoDesafioAtivo()).isEqualTo(placarAntes + 20);
        // As 24 chamadas somam exatamente os mesmos 20 de XP que o item e o desafio pagaram —
        // se o lock pessimista do Usuario não estivesse lá, esse valor viria menor que 20.
        assertThat(xpDoUsuario()).isEqualTo(xpUsuarioAntes + 20);
        // No clube vale a mesma conta: um ponto por página nova, pago uma única vez.
        assertThat(pontosNoClube()).isEqualTo(20);
        // Vinte e quatro registros no mesmo dia contam um dia só.
        assertThat(sequencia()).isEqualTo(13);
        assertThat(ultimaLeitura()).isEqualTo(SequenciaDeLeitura.hoje());
    }

    @Test
    void deve_preservar_pontos_somados_por_outra_transacao_durante_o_registro() throws Exception {
        TransactionTemplate transacao = new TransactionTemplate(transactionManager);
        CountDownLatch lockAdquirido = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Faz o que o quiz faz em um acerto: pega o lock da linha do membro e soma 50 pontos.
            // Só confirma depois que o registro de leitura estiver esperando por essa linha.
            Future<?> quiz =
                    executor.submit(
                            () ->
                                    transacao.executeWithoutResult(
                                            status -> {
                                                membroClubeRepository
                                                        .buscarComLock(CLUBE, USUARIO)
                                                        .orElseThrow()
                                                        .somarPontos(50);
                                                lockAdquirido.countDown();
                                                esperarTransacaoBloqueada();
                                            }));
            assertThat(lockAdquirido.await(10, TimeUnit.SECONDS)).isTrue();

            service.registrar(LIVRO, new RegistrarProgressoRequest(10));
            quiz.get(30, TimeUnit.SECONDS);
        }

        // Sem o lock na leitura dos membros, o registro gravaria seus 10 pontos por cima dos 50 do
        // quiz, em vez de somar aos dois.
        assertThat(pontosNoClube()).isEqualTo(60);
    }

    private void esperarTransacaoBloqueada() {
        for (int tentativa = 0; tentativa < 200; tentativa++) {
            Integer bloqueadas =
                    jdbc.queryForObject(
                            "SELECT count(*) FROM pg_locks WHERE NOT granted", Integer.class);
            if (bloqueadas > 0) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
        }
        throw new IllegalStateException("O registro de leitura não esperou pelo lock do membro.");
    }

    private int pontosNoClube() {
        return jdbc.queryForObject(
                "SELECT pontos FROM membro_clube WHERE clube_id = ? AND usuario_id = ?",
                Integer.class,
                CLUBE,
                USUARIO);
    }

    private int sequencia() {
        return jdbc.queryForObject(
                "SELECT dias_consecutivos FROM usuario WHERE id = ?", Integer.class, USUARIO);
    }

    private LocalDate ultimaLeitura() {
        return jdbc.queryForObject(
                "SELECT ultima_leitura_em FROM usuario WHERE id = ?", LocalDate.class, USUARIO);
    }

    private int placarDoDesafioAtivo() {
        return jdbc.queryForObject(
                "SELECT valor_atual FROM progresso_desafio"
                        + " WHERE desafio_id = ? AND usuario_id = ?",
                Integer.class,
                DESAFIO_ATIVO,
                USUARIO);
    }

    private int xpDoUsuario() {
        return jdbc.queryForObject(
                "SELECT xp_total FROM usuario WHERE id = ?", Integer.class, USUARIO);
    }

    private int nivelDoUsuario() {
        return jdbc.queryForObject(
                "SELECT nivel FROM usuario WHERE id = ?", Integer.class, USUARIO);
    }

    private void verificarChamadasConcorrentes(int pagina, int xpEsperado) throws Exception {
        CountDownLatch prontas = new CountDownLatch(12);
        CountDownLatch inicio = new CountDownLatch(1);
        List<Future<ProgressoLeituraResponse>> chamadas = new ArrayList<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 12; i++) {
                chamadas.add(
                        executor.submit(
                                () -> {
                                    prontas.countDown();
                                    if (!inicio.await(10, TimeUnit.SECONDS))
                                        throw new IllegalStateException("Início não liberado");
                                    return service.registrar(
                                            LIVRO, new RegistrarProgressoRequest(pagina));
                                }));
            }
            try {
                assertThat(prontas.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                inicio.countDown();
            }
            int xpTotal = 0;
            for (var chamada : chamadas) {
                var resposta = chamada.get(30, TimeUnit.SECONDS);
                assertThat(resposta.paginaAtual()).isEqualTo(pagina);
                assertThat(resposta.paginaMaximaAlcancada()).isEqualTo(pagina);
                assertThat(resposta.sequenciaDias()).isEqualTo(13);
                xpTotal += resposta.xpTotal();
            }
            assertThat(xpTotal).isEqualTo(xpEsperado);
        }
    }
}
