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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.dto.request.RegistrarProgressoRequest;
import com.readrace.api.dto.response.ProgressoLeituraResponse;
import com.readrace.api.model.SequenciaDeLeitura;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistrarProgressoConcorrenciaIT {
    private static final UUID LIVRO = UUID.fromString("30000000-0000-0000-0000-000000000013");
    private static final UUID USUARIO = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DESAFIO_ATIVO =
            UUID.fromString("90000000-0000-0000-0000-000000000001");

    @Autowired private RegistrarProgressoService service;
    @Autowired private JdbcTemplate jdbc;

    private int placarAntes;
    private int sequenciaAntes;
    private LocalDate ultimaLeituraAntes;

    @BeforeEach
    void guardar_estado_do_usuario() {
        placarAntes = placarDoDesafioAtivo();
        sequenciaAntes = sequencia();
        ultimaLeituraAntes = ultimaLeitura();

        // Parte de um estado conhecido: leu ontem e está com sequência 12.
        jdbc.update(
                "UPDATE usuario SET dias_consecutivos = 12, ultima_leitura_em = ? WHERE id = ?",
                SequenciaDeLeitura.hoje().minusDays(1),
                USUARIO);
    }

    @AfterEach
    void limpar_registros_confirmados() {
        // Os registros também avançam o desafio ativo e a sequência do usuário e, aqui, são
        // confirmados de verdade.
        jdbc.update(
                "UPDATE progresso_desafio SET valor_atual = ?"
                        + " WHERE desafio_id = ? AND usuario_id = ?",
                placarAntes,
                DESAFIO_ATIVO,
                USUARIO);
        jdbc.update(
                "UPDATE usuario SET dias_consecutivos = ?, ultima_leitura_em = ? WHERE id = ?",
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
        // Vinte e quatro registros no mesmo dia contam um dia só.
        assertThat(sequencia()).isEqualTo(13);
        assertThat(ultimaLeitura()).isEqualTo(SequenciaDeLeitura.hoje());
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
