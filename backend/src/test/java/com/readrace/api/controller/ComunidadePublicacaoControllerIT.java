package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.readrace.api.TestcontainersConfiguration;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ComunidadePublicacaoControllerIT {
    private static final UUID EU = UUID.fromString("00000000-0000-0000-0000-000000000001");
    @Autowired private MockMvcTester mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private ObjectMapper mapper;

    @ParameterizedTest
    @ValueSource(strings = {"membro", "administrador"})
    void participante_publica_raiz_com_autoria_autenticada_e_aparece_no_topo(String cargo)
            throws Exception {
        UUID comunidade = criarComunidade(cargo);
        UUID antigo = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO post (id, autor_id, comunidade_id, conteudo, criado_em)"
                        + " VALUES (?, ?, ?, 'Post anterior', TIMESTAMPTZ '2020-01-01 00:00:00+00')",
                antigo,
                EU,
                comunidade);
        var usuarioAntes = jdbc.queryForMap("SELECT * FROM usuario WHERE id = ?", EU);
        var vinculoAntes =
                jdbc.queryForMap(
                        "SELECT * FROM membro_comunidade WHERE comunidade_id = ? AND usuario_id = ?",
                        comunidade,
                        EU);
        int antes = totalPosts();
        String texto = " \t\u00A0 Minha leitura\r\ncontinua aqui. 😀\n ";
        var autor = jdbc.queryForMap("SELECT nome, avatar_url FROM usuario WHERE id = ?", EU);
        OffsetDateTime inicio = OffsetDateTime.now();

        MvcTestResult resposta =
                enviar(
                        comunidade,
                        mapper.writeValueAsString(
                                Map.of(
                                        "conteudo",
                                        texto,
                                        "autorId",
                                        "00000000-0000-0000-0000-000000000002")));

        assertThat(resposta).hasStatus(HttpStatus.CREATED);
        Map<String, Object> criado =
                JsonPath.read(resposta.getResponse().getContentAsString(), "$");
        assertThat(criado)
                .containsOnlyKeys(
                        "id",
                        "autor",
                        "publicadoEm",
                        "texto",
                        "totalCurtidas",
                        "curtidoPorMim",
                        "totalComentarios")
                .containsEntry("texto", texto)
                .containsEntry("totalCurtidas", 0)
                .containsEntry("curtidoPorMim", false)
                .containsEntry("totalComentarios", 0);
        assertThat((Map<?, ?>) criado.get("autor"))
                .isEqualTo(
                        Map.of(
                                "id", EU.toString(),
                                "nome", autor.get("nome"),
                                "avatarUrl", autor.get("avatar_url")));
        assertThat(OffsetDateTime.parse((String) criado.get("publicadoEm")))
                .isBetween(inicio, OffsetDateTime.now());
        UUID postId = UUID.fromString((String) criado.get("id"));
        entityManager.flush();
        assertThat(totalPosts()).isEqualTo(antes + 1);
        assertThat(jdbc.queryForMap("SELECT * FROM post WHERE id = ?", postId))
                .containsEntry("autor_id", EU)
                .containsEntry("comunidade_id", comunidade)
                .containsEntry("clube_id", null)
                .containsEntry("livro_id", null)
                .containsEntry("post_pai_id", null)
                .containsEntry("excluido_em", null)
                .containsEntry("conteudo", texto);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM curtida WHERE post_id = ?",
                                Integer.class,
                                postId))
                .isZero();

        MvcTestResult perfil = mvc.get().uri("/api/comunidades/" + comunidade).exchange();
        assertThat(perfil).hasStatusOk();
        String corpo = perfil.getResponse().getContentAsString();
        assertThat(JsonPath.<Map<String, Object>>read(corpo, "$.posts[0]")).isEqualTo(criado);
        assertThat(JsonPath.<String>read(corpo, "$.posts[1].id")).isEqualTo(antigo.toString());
        assertThat(jdbc.queryForMap("SELECT * FROM usuario WHERE id = ?", EU))
                .isEqualTo(usuarioAntes);
        assertThat(
                        jdbc.queryForMap(
                                "SELECT * FROM membro_comunidade WHERE comunidade_id = ? AND usuario_id = ?",
                                comunidade,
                                EU))
                .isEqualTo(vinculoAntes);
    }

    @Test
    void nao_participante_recebe_403_sem_criar_post() {
        UUID comunidade = criarComunidade(null);
        int antes = totalPosts();

        assertThat(enviar(comunidade, "{\"conteudo\":\"Minha leitura\"}"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("code", "SO_MEMBRO_POSTA")
                .containsEntry("message", "Somente participantes da comunidade podem publicar.");

        entityManager.flush();
        assertThat(totalPosts()).isEqualTo(antes);
    }

    @ParameterizedTest
    @MethodSource("conteudosInvalidos")
    void rejeita_conteudo_invalido_sem_criar_post(String json) {
        UUID comunidade = criarComunidade("membro");
        int antes = totalPosts();

        assertThat(enviar(comunidade, json))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("POST_INVALIDO");

        entityManager.flush();
        assertThat(totalPosts()).isEqualTo(antes);
    }

    static Stream<String> conteudosInvalidos() {
        return Stream.of(
                "{}",
                "{\"conteudo\":null}",
                "{\"conteudo\":\"\"}",
                "{\"conteudo\":\" \\t\\r\\n\u00A0 \"}",
                "{\"conteudo\":\"" + "a".repeat(1999) + "😀\"}");
    }

    @Test
    void aceita_2000_unidades_utf16() throws Exception {
        UUID comunidade = criarComunidade("membro");
        String conteudo = "a".repeat(1998) + "😀";

        assertThat(enviar(comunidade, mapper.writeValueAsString(Map.of("conteudo", conteudo))))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.texto")
                .isEqualTo(conteudo);
        entityManager.flush();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT conteudo FROM post WHERE comunidade_id = ?",
                                String.class,
                                comunidade))
                .isEqualTo(conteudo);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void comunidade_inexistente_ou_excluida_retorna_404_sem_criar_post(boolean excluida) {
        UUID comunidade = excluida ? criarComunidade("membro") : UUID.randomUUID();
        if (excluida) {
            jdbc.update("UPDATE comunidade SET excluido_em = now() WHERE id = ?", comunidade);
        }
        int antes = totalPosts();

        assertThat(enviar(comunidade, "{\"conteudo\":\"Minha leitura\"}"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("code", "COMUNIDADE_NAO_ENCONTRADA")
                .containsEntry("message", "Comunidade não encontrada.");

        entityManager.flush();
        assertThat(totalPosts()).isEqualTo(antes);
    }

    private UUID criarComunidade(String cargo) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO comunidade (id, nome) VALUES (?, 'Publicação')", id);
        if (cargo != null) {
            jdbc.update(
                    "INSERT INTO membro_comunidade (id, comunidade_id, usuario_id, cargo_comunidade)"
                            + " VALUES (?, ?, ?, CAST(? AS cargo))",
                    UUID.randomUUID(),
                    id,
                    EU,
                    cargo);
        }
        return id;
    }

    private MvcTestResult enviar(UUID comunidade, String json) {
        return mvc.post()
                .uri("/api/comunidades/" + comunidade + "/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private int totalPosts() {
        return jdbc.queryForObject("SELECT count(*) FROM post", Integer.class);
    }
}
