package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.dto.response.SeguirResponse;
import com.readrace.api.service.DesafioService;
import com.readrace.api.service.PerfilService;
import com.readrace.api.service.SeguirService;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class SeguirControllerIT {
    private static final UUID EU = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OUTRO = UUID.fromString("00000000-0000-0000-0000-000000000002");
    @Autowired private MockMvcTester mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PerfilService perfis;
    @Autowired private DesafioService desafios;
    @Autowired private SeguirService seguir;

    private String url(UUID id) {
        return "/api/usuarios/" + id + "/seguidores/eu";
    }

    private void limparRelacoes() {
        jdbc.update(
                "DELETE FROM seguir WHERE (seguidor_id = ? AND seguido_id = ?) OR (seguidor_id = ? AND seguido_id = ?)",
                EU,
                OUTRO,
                OUTRO,
                EU);
    }

    @Test
    void seguirEDeixarDeSeguirSaoIdempotentesSemDarXp() {
        limparRelacoes();
        var antes = perfis.buscar(OUTRO);
        int xpEu = perfis.buscarDoUsuarioAtual().xpAtual();
        for (int i = 0; i < 2; i++) {
            assertThat(mvc.post().uri(url(OUTRO)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$")
                    .asMap()
                    .containsEntry("seguidores", (int) antes.seguidores() + 1)
                    .containsEntry("sigo", true)
                    .containsEntry("meSegue", false);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM seguir WHERE seguidor_id = ? AND seguido_id = ?",
                                Integer.class,
                                EU,
                                OUTRO))
                .isEqualTo(1);
        assertThat(perfis.buscar(OUTRO).sigo()).isTrue();
        assertThat(perfis.buscar(OUTRO).seguindo()).isEqualTo(antes.seguindo());
        assertThat(perfis.buscarDoUsuarioAtual().xpAtual()).isEqualTo(xpEu);
        assertThat(perfis.buscar(OUTRO).xpAtual()).isEqualTo(antes.xpAtual());
        for (int i = 0; i < 2; i++) {
            assertThat(mvc.delete().uri(url(OUTRO)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$")
                    .asMap()
                    .containsEntry("seguidores", (int) antes.seguidores())
                    .containsEntry("sigo", false)
                    .containsEntry("meSegue", false);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM seguir WHERE seguidor_id = ? AND seguido_id = ?",
                                Integer.class,
                                EU,
                                OUTRO))
                .isZero();
        assertThat(perfis.buscarDoUsuarioAtual().xpAtual()).isEqualTo(xpEu);
    }

    @Test
    void seguirDeVoltaCriaAmizadeEDeixarDeSeguirRemoveSoUmaDirecao() {
        // No seed os dois já se seguem. Remover uma direção prepara "Seguir de volta".
        assertThat(mvc.delete().uri(url(OUTRO))).hasStatusOk();
        assertThat(mvc.get().uri("/api/usuarios/" + OUTRO + "/perfil"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("sigo", false)
                .containsEntry("meSegue", true);
        assertThat(desafios.listarOponentes(null).oponentes()).noneMatch(o -> o.id().equals(OUTRO));
        assertThat(mvc.post().uri(url(OUTRO)))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("sigo", true)
                .containsEntry("meSegue", true);
        assertThat(desafios.listarOponentes(null).oponentes()).anyMatch(o -> o.id().equals(OUTRO));
        assertThat(mvc.delete().uri(url(OUTRO))).hasStatusOk();
        assertThat(desafios.listarOponentes(null).oponentes()).noneMatch(o -> o.id().equals(OUTRO));
        assertThat(perfis.buscar(OUTRO).meSegue()).isTrue();
    }

    @Test
    void relacaoUnilateralNaoCriaAmigo() {
        limparRelacoes();
        assertThat(mvc.post().uri(url(OUTRO))).hasStatusOk();
        assertThat(desafios.listarOponentes(null).oponentes()).noneMatch(o -> o.id().equals(OUTRO));
    }

    @Test
    void rejeitaAutoSeguimentoNosDoisMetodos() {
        assertThat(mvc.post().uri(url(EU)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("NAO_PODE_SEGUIR_A_SI");
        assertThat(mvc.delete().uri(url(EU)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("NAO_PODE_SEGUIR_A_SI");
        var proprio = perfis.buscarDoUsuarioAtual();
        assertThat(proprio.sigo()).isFalse();
        assertThat(proprio.meSegue()).isFalse();
    }

    @Test
    void usuarioInexistenteOuExcluidoRetorna404() {
        verificarNaoEncontrado(UUID.randomUUID());
        jdbc.update("UPDATE usuario SET excluido_em = now() WHERE id = ?", OUTRO);
        verificarNaoEncontrado(OUTRO);
    }

    private void verificarNaoEncontrado(UUID id) {
        assertThat(mvc.post().uri(url(id)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("USUARIO_NAO_ENCONTRADO");
        assertThat(mvc.delete().uri(url(id)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("USUARIO_NAO_ENCONTRADO");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void chamadasConcorrentesNaoDuplicamRelacao() throws Exception {
        UUID alvo = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO usuario (id, nome, nome_usuario, email) VALUES (?, 'Concorrência', ?, ?)",
                alvo,
                alvo.toString(),
                alvo + "@example.com");
        try {
            for (boolean criar : new boolean[] {true, false}) {
                try (var executor = Executors.newFixedThreadPool(6)) {
                    var inicio = new CountDownLatch(1);
                    var prontos = new CountDownLatch(6);
                    var respostas = new ArrayList<Future<SeguirResponse>>();
                    for (int i = 0; i < 6; i++) {
                        respostas.add(
                                executor.submit(
                                        () -> {
                                            prontos.countDown();
                                            if (!inicio.await(10, TimeUnit.SECONDS))
                                                throw new IllegalStateException(
                                                        "Início não liberado");
                                            return criar
                                                    ? seguir.seguir(alvo)
                                                    : seguir.deixarDeSeguir(alvo);
                                        }));
                    }
                    assertThat(prontos.await(10, TimeUnit.SECONDS)).isTrue();
                    inicio.countDown();
                    for (var resposta : respostas) {
                        assertThat(resposta.get(20, TimeUnit.SECONDS))
                                .isEqualTo(new SeguirResponse(criar ? 1 : 0, criar, false));
                    }
                }
                assertThat(
                                jdbc.queryForObject(
                                        "SELECT count(*) FROM seguir WHERE seguido_id = ?",
                                        Integer.class,
                                        alvo))
                        .isEqualTo(criar ? 1 : 0);
            }
        } finally {
            jdbc.update("DELETE FROM seguir WHERE seguido_id = ?", alvo);
            jdbc.update("DELETE FROM usuario WHERE id = ?", alvo);
        }
    }
}
