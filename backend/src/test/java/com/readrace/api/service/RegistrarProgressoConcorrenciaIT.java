package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;

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
    private int xpUsuarioAntes;
    private int nivelUsuarioAntes;

    @BeforeEach
    void guardar_placar_do_desafio_ativo() {
        placarAntes = placarDoDesafioAtivo();
        xpUsuarioAntes = xpDoUsuario();
        nivelUsuarioAntes = nivelDoUsuario();
    }

    @AfterEach
    void limpar_registros_confirmados() {
        // Os registros também avançam o desafio ativo e o XP do usuário do seed, e aqui são
        // confirmados de verdade — sem isso, os testes seguintes herdariam o estado sujo.
        jdbc.update(
                "UPDATE progresso_desafio SET valor_atual = ?"
                        + " WHERE desafio_id = ? AND usuario_id = ?",
                placarAntes,
                DESAFIO_ATIVO,
                USUARIO);
        jdbc.update(
                "UPDATE usuario SET xp_total = ?, nivel = ? WHERE id = ?",
                xpUsuarioAntes,
                nivelUsuarioAntes,
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
        // As 24 chamadas somam exatamente os mesmos 20 de XP que o item e o desafio pagaram —
        // se o lock pessimista do Usuario não estivesse lá, esse valor viria menor que 20.
        assertThat(xpDoUsuario()).isEqualTo(xpUsuarioAntes + 20);
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
                xpTotal += resposta.xpTotal();
            }
            assertThat(xpTotal).isEqualTo(xpEsperado);
        }
    }
}
