package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("POST e DELETE /api/posts/{postId}/curtida")
class CurtidaControllerIT {

    // Usuário atual do seed (readrace.usuario-seed.id). No seed (V4) ele já curtiu todo post de
    // clube/comunidade, então os testes criam um post novo, sempre sem curtida deste usuário.
    private static final UUID USUARIO_ATUAL =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OUTRO_USUARIO =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID LIVRO_DOM_CASMURRO =
            UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Autowired private MockMvcTester mvc;
    @Autowired private JdbcTemplate jdbc;

    private UUID criarPostSemCurtidas() {
        UUID postId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO post (id, autor_id, livro_id, conteudo) VALUES (?, ?, ?, 'Post de"
                        + " teste')",
                postId,
                OUTRO_USUARIO,
                LIVRO_DOM_CASMURRO);
        return postId;
    }

    private String url(UUID postId) {
        return "/api/posts/" + postId + "/curtida";
    }

    private long curtidasNoBanco(UUID postId, UUID usuarioId) {
        Integer total =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM curtida WHERE post_id = ? AND usuario_id = ?",
                        Integer.class,
                        postId,
                        usuarioId);
        return total == null ? 0 : total;
    }

    @Test
    @DisplayName("curtir um post ainda não curtido incrementa a contagem")
    void deve_curtir_um_post_nao_curtido() {
        UUID postId = criarPostSemCurtidas();

        assertThat(mvc.post().uri(url(postId)))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("curtidoPorMim", true)
                .containsEntry("totalCurtidas", 1);
        assertThat(curtidasNoBanco(postId, USUARIO_ATUAL)).isEqualTo(1);
    }

    @Test
    @DisplayName("curtir duas vezes não cria curtida duplicada e continua devolvendo 200")
    void deve_ser_idempotente_ao_curtir_duas_vezes() {
        UUID postId = criarPostSemCurtidas();

        assertThat(mvc.post().uri(url(postId))).hasStatus(HttpStatus.OK);
        assertThat(mvc.post().uri(url(postId)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.totalCurtidas")
                .isEqualTo(1);

        assertThat(curtidasNoBanco(postId, USUARIO_ATUAL)).isEqualTo(1);
    }

    @Test
    @DisplayName("descurtir remove a curtida existente")
    void deve_descurtir_um_post_curtido() {
        UUID postId = criarPostSemCurtidas();
        mvc.post().uri(url(postId)).exchange();

        assertThat(mvc.delete().uri(url(postId)))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.curtidoPorMim")
                .isEqualTo(false);
        assertThat(curtidasNoBanco(postId, USUARIO_ATUAL)).isEqualTo(0);
    }

    @Test
    @DisplayName("descurtir um post nunca curtido devolve 200 sem erro")
    void deve_ser_idempotente_ao_descurtir_post_nunca_curtido() {
        UUID postId = criarPostSemCurtidas();

        assertThat(mvc.delete().uri(url(postId)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.totalCurtidas")
                .isEqualTo(0);
    }

    @Test
    @DisplayName("o usuário pode curtir o próprio post")
    void deve_permitir_curtir_o_proprio_post() {
        UUID postId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO post (id, autor_id, livro_id, conteudo) VALUES (?, ?, ?, 'Post"
                        + " próprio')",
                postId,
                USUARIO_ATUAL,
                LIVRO_DOM_CASMURRO);

        assertThat(mvc.post().uri(url(postId)))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.curtidoPorMim")
                .isEqualTo(true);
    }

    @Test
    @DisplayName("curtir post inexistente devolve 404 no formato padrão")
    void deve_devolver_404_ao_curtir_post_inexistente() {
        assertThat(mvc.post().uri(url(UUID.randomUUID())))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("descurtir post inexistente devolve 404 no formato padrão")
    void deve_devolver_404_ao_descurtir_post_inexistente() {
        assertThat(mvc.delete().uri(url(UUID.randomUUID())))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }
}
