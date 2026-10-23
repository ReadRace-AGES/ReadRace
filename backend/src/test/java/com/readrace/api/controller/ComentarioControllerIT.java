package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
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

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("GET e POST /api/posts/{postId}/comentarios")
class ComentarioControllerIT {
    private static final UUID EU = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OUTRO = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID TERCEIRO = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID CLUBE = UUID.fromString("50000000-0000-0000-0000-000000000001");

    @Autowired private MockMvcTester mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private ObjectMapper mapper;

    /** Comunidade nova com um post raiz de {@code OUTRO}; {@code EU} segue se pedido. */
    private UUID postDeComunidade(boolean sigo) {
        UUID comunidade = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO comunidade (id, nome) VALUES (?, 'Comunidade de teste')", comunidade);
        if (sigo) {
            jdbc.update(
                    "INSERT INTO membro_comunidade (id, comunidade_id, usuario_id, cargo_comunidade)"
                            + " VALUES (?, ?, ?, 'membro')",
                    UUID.randomUUID(),
                    comunidade,
                    EU);
        }
        UUID post = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO post (id, autor_id, comunidade_id, conteudo) VALUES (?, ?, ?, 'Post')",
                post,
                OUTRO,
                comunidade);
        return post;
    }

    private UUID comentarioDireto(UUID postId, UUID autorId, String texto, String atras) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO post (id, autor_id, post_pai_id, conteudo, criado_em)"
                        + " VALUES (?, ?, ?, ?, now() - CAST(? AS interval))",
                id,
                autorId,
                postId,
                texto,
                atras);
        return id;
    }

    private static String url(UUID postId) {
        return "/api/posts/" + postId + "/comentarios";
    }

    private MvcTestResult comentar(UUID postId, String json) {
        return mvc.post()
                .uri(url(postId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private int totalComentarios(UUID postId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM post WHERE post_pai_id = ?", Integer.class, postId);
    }

    @Test
    void lista_do_mais_antigo_para_o_mais_novo_com_autor() {
        UUID post = postDeComunidade(true);
        UUID novo = comentarioDireto(post, TERCEIRO, "Segundo", "1 hour");
        UUID antigo = comentarioDireto(post, OUTRO, "Primeiro", "2 hours");

        MvcTestResult resposta = mvc.get().uri(url(post)).exchange();

        assertThat(resposta).hasStatusOk();
        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.comentarios[*].id")
                .isEqualTo(List.of(antigo.toString(), novo.toString()));
        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.comentarios[0]")
                .asMap()
                .containsEntry("texto", "Primeiro")
                .containsKey("publicadoEm");
        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.comentarios[0].autor.id")
                .isEqualTo(OUTRO.toString());
        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.comentarios[0].autor")
                .asMap()
                .containsKeys("nome", "avatarUrl");
    }

    @Test
    void nao_lista_comentario_excluido() {
        UUID post = postDeComunidade(true);
        UUID excluido = comentarioDireto(post, OUTRO, "Apagado", "1 hour");
        jdbc.update("UPDATE post SET excluido_em = now() WHERE id = ?", excluido);

        assertThat(mvc.get().uri(url(post)))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.comentarios")
                .asArray()
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void informa_podeComentar_e_deixa_quem_nao_segue_ler(boolean sigo) {
        UUID post = postDeComunidade(sigo);
        comentarioDireto(post, OUTRO, "Visível para todos", "1 hour");

        MvcTestResult resposta = mvc.get().uri(url(post)).exchange();

        assertThat(resposta).hasStatusOk();
        assertThat(resposta).bodyJson().extractingPath("$.podeComentar").isEqualTo(sigo);
        assertThat(resposta).bodyJson().extractingPath("$.comentarios").asArray().hasSize(1);
    }

    @Test
    void comenta_como_post_com_pai_e_sem_escopo_proprio() throws Exception {
        UUID post = postDeComunidade(true);

        MvcTestResult resposta = comentar(post, "{\"texto\":\"Meu comentário\"}");

        assertThat(resposta).hasStatus(HttpStatus.CREATED);
        assertThat(resposta).bodyJson().extractingPath("$.texto").isEqualTo("Meu comentário");
        assertThat(resposta).bodyJson().extractingPath("$.autor.id").isEqualTo(EU.toString());
        entityManager.flush();
        UUID id =
                UUID.fromString(JsonPath.read(resposta.getResponse().getContentAsString(), "$.id"));
        Map<String, Object> linha =
                jdbc.queryForMap(
                        "SELECT autor_id, post_pai_id, clube_id, comunidade_id, livro_id, conteudo"
                                + " FROM post WHERE id = ?",
                        id);
        assertThat(linha)
                .containsEntry("autor_id", EU)
                .containsEntry("post_pai_id", post)
                .containsEntry("clube_id", null)
                .containsEntry("comunidade_id", null)
                .containsEntry("livro_id", null)
                .containsEntry("conteudo", "Meu comentário");
    }

    @Test
    void comentario_novo_aparece_no_fim_da_lista_e_soma_no_perfil_da_comunidade() {
        UUID post = postDeComunidade(true);
        comentarioDireto(post, OUTRO, "Antigo", "1 hour");
        UUID comunidade =
                jdbc.queryForObject(
                        "SELECT comunidade_id FROM post WHERE id = ?", UUID.class, post);

        assertThat(comentar(post, "{\"texto\":\"Novo\"}")).hasStatus(HttpStatus.CREATED);
        entityManager.flush();

        assertThat(mvc.get().uri(url(post)))
                .bodyJson()
                .extractingPath("$.comentarios[*].texto")
                .isEqualTo(List.of("Antigo", "Novo"));
        assertThat(mvc.get().uri("/api/comunidades/" + comunidade))
                .bodyJson()
                .extractingPath("$.posts[0].totalComentarios")
                .isEqualTo(2);
    }

    @Test
    void quem_nao_segue_nao_comenta() {
        UUID post = postDeComunidade(false);

        assertThat(comentar(post, "{\"texto\":\"Tentativa\"}"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("code", "SO_QUEM_SEGUE_COMENTA")
                .containsEntry("message", "Siga a comunidade para comentar.");
        entityManager.flush();
        assertThat(totalComentarios(post)).isZero();
    }

    @ParameterizedTest
    @MethodSource("corposInvalidos")
    void valida_o_texto(String json) {
        UUID post = postDeComunidade(true);

        assertThat(comentar(post, json))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("code", "COMENTARIO_INVALIDO")
                .containsEntry(
                        "message",
                        "O comentário deve conter texto e ter no máximo 1.000 caracteres.");
        entityManager.flush();
        assertThat(totalComentarios(post)).isZero();
    }

    static Stream<String> corposInvalidos() {
        return Stream.of(
                "{}",
                "{\"texto\":null}",
                "{\"texto\":\"\"}",
                "{\"texto\":\" \\t\\r\\n  \"}",
                "{\"texto\":\"" + "a".repeat(1001) + "\"}");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 1000})
    void aceita_os_limites_do_texto(int tamanho) throws Exception {
        UUID post = postDeComunidade(true);
        String texto = "a".repeat(tamanho);

        assertThat(comentar(post, mapper.writeValueAsString(Map.of("texto", texto))))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.texto")
                .isEqualTo(texto);
    }

    @ParameterizedTest
    @MethodSource("postsQueNaoAceitamComentario")
    void responde_404_para_post_que_nao_aceita_comentario(String caso) {
        UUID post = postQueNaoAceita(caso);

        assertThat(mvc.get().uri(url(post)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("code", "POST_NAO_ENCONTRADO")
                .containsEntry("message", "Post não encontrado.");
        assertThat(comentar(post, "{\"texto\":\"Oi\"}"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("POST_NAO_ENCONTRADO");
    }

    static Stream<String> postsQueNaoAceitamComentario() {
        return Stream.of(
                "inexistente",
                "excluido",
                "de clube",
                "comentario em post de clube",
                "resposta abaixo de comentario excluido",
                "de comunidade excluida");
    }

    private UUID postQueNaoAceita(String caso) {
        return switch (caso) {
            case "inexistente" -> UUID.randomUUID();
            case "excluido" -> {
                UUID post = postDeComunidade(true);
                jdbc.update("UPDATE post SET excluido_em = now() WHERE id = ?", post);
                yield post;
            }
            case "de clube" -> {
                UUID post = UUID.randomUUID();
                jdbc.update(
                        "INSERT INTO post (id, autor_id, clube_id, conteudo) VALUES (?, ?, ?, 'Clube')",
                        post,
                        EU,
                        CLUBE);
                yield post;
            }
            case "comentario em post de clube" -> {
                UUID post = UUID.randomUUID();
                jdbc.update(
                        "INSERT INTO post (id, autor_id, clube_id, conteudo) VALUES (?, ?, ?, 'Clube')",
                        post,
                        EU,
                        CLUBE);
                yield comentarioDireto(post, OUTRO, "No clube", "1 hour");
            }
            case "resposta abaixo de comentario excluido" -> {
                UUID pai = comentarioDireto(postDeComunidade(true), OUTRO, "Pai", "2 hours");
                UUID resposta = comentarioDireto(pai, TERCEIRO, "Resposta", "1 hour");
                jdbc.update("UPDATE post SET excluido_em = now() WHERE id = ?", pai);
                yield resposta;
            }
            case "de comunidade excluida" -> {
                UUID post = postDeComunidade(true);
                jdbc.update(
                        "UPDATE comunidade SET excluido_em = now()"
                                + " WHERE id = (SELECT comunidade_id FROM post WHERE id = ?)",
                        post);
                yield post;
            }
            default -> throw new IllegalArgumentException(caso);
        };
    }

    @Test
    void mantem_tratamento_de_json_malformado() {
        UUID post = postDeComunidade(true);

        assertThat(comentar(post, "{\"texto\":"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    void responde_a_um_comentario_e_lista_a_thread_em_arvore() throws Exception {
        UUID post = postDeComunidade(true);
        UUID primeiro = comentarioDireto(post, OUTRO, "Primeiro", "3 hours");
        UUID segundo = comentarioDireto(post, TERCEIRO, "Segundo", "2 hours");
        UUID respostaAntiga =
                comentarioDireto(primeiro, TERCEIRO, "Resposta antiga", "150 minutes");

        MvcTestResult criada = comentar(primeiro, "{\"texto\":\"Minha resposta\"}");

        assertThat(criada).hasStatus(HttpStatus.CREATED);
        assertThat(criada).bodyJson().extractingPath("$.respostas").asArray().isEmpty();
        assertThat(criada).bodyJson().extractingPath("$.totalCurtidas").isEqualTo(0);
        entityManager.flush();
        UUID minha =
                UUID.fromString(JsonPath.read(criada.getResponse().getContentAsString(), "$.id"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT post_pai_id FROM post WHERE id = ?", UUID.class, minha))
                .isEqualTo(primeiro);
        UUID netinha = comentarioDireto(minha, OUTRO, "Resposta da resposta", "0 minutes");

        MvcTestResult thread = mvc.get().uri(url(post)).exchange();

        assertThat(thread).hasStatusOk();
        assertThat(thread)
                .bodyJson()
                .extractingPath("$.comentarios[*].id")
                .isEqualTo(List.of(primeiro.toString(), segundo.toString()));
        assertThat(thread)
                .bodyJson()
                .extractingPath("$.comentarios[0].respostas[*].id")
                .isEqualTo(List.of(respostaAntiga.toString(), minha.toString()));
        assertThat(thread)
                .bodyJson()
                .extractingPath("$.comentarios[0].respostas[1].respostas[*].id")
                .isEqualTo(List.of(netinha.toString()));
        assertThat(thread)
                .bodyJson()
                .extractingPath("$.comentarios[1].respostas")
                .asArray()
                .isEmpty();
        assertThat(mvc.get().uri(url(primeiro)))
                .bodyJson()
                .extractingPath("$.comentarios[*].id")
                .isEqualTo(List.of(respostaAntiga.toString(), minha.toString()));
    }

    @Test
    void comentario_excluido_esconde_as_respostas_dele() {
        UUID post = postDeComunidade(true);
        UUID pai = comentarioDireto(post, OUTRO, "Pai", "2 hours");
        comentarioDireto(pai, TERCEIRO, "Resposta", "1 hour");
        jdbc.update("UPDATE post SET excluido_em = now() WHERE id = ?", pai);

        assertThat(mvc.get().uri(url(post)))
                .bodyJson()
                .extractingPath("$.comentarios")
                .asArray()
                .isEmpty();
        assertThat(mvc.get().uri("/api/comunidades/" + comunidadeDe(post)))
                .bodyJson()
                .extractingPath("$.posts[0].totalComentarios")
                .isEqualTo(0);
    }

    @Test
    void quem_nao_segue_nao_responde() {
        UUID post = postDeComunidade(false);
        UUID comentario = comentarioDireto(post, OUTRO, "Pai", "1 hour");

        assertThat(comentar(comentario, "{\"texto\":\"Resposta\"}"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("SO_QUEM_SEGUE_COMENTA");
        entityManager.flush();
        assertThat(totalComentarios(comentario)).isZero();
    }

    @Test
    void comentario_tem_curtidas_pela_rota_do_post() {
        UUID post = postDeComunidade(true);
        UUID comentario = comentarioDireto(post, OUTRO, "Curta", "1 hour");
        jdbc.update(
                "INSERT INTO curtida (id, post_id, usuario_id) VALUES (?, ?, ?)",
                UUID.randomUUID(),
                comentario,
                TERCEIRO);

        assertThat(mvc.post().uri("/api/posts/" + comentario + "/curtida"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalCurtidas")
                .isEqualTo(2);
        entityManager.flush();

        MvcTestResult thread = mvc.get().uri(url(post)).exchange();
        assertThat(thread).bodyJson().extractingPath("$.comentarios[0].totalCurtidas").isEqualTo(2);
        assertThat(thread)
                .bodyJson()
                .extractingPath("$.comentarios[0].curtidoPorMim")
                .isEqualTo(true);
    }

    @Test
    void total_de_comentarios_do_post_soma_as_respostas_de_todos_os_niveis() {
        UUID post = postDeComunidade(true);
        UUID pai = comentarioDireto(post, OUTRO, "Pai", "3 hours");
        UUID filha = comentarioDireto(pai, TERCEIRO, "Filha", "2 hours");
        comentarioDireto(filha, OUTRO, "Neta", "1 hour");
        comentarioDireto(post, TERCEIRO, "Outro", "1 hour");

        assertThat(mvc.get().uri("/api/comunidades/" + comunidadeDe(post)))
                .bodyJson()
                .extractingPath("$.posts[0].totalComentarios")
                .isEqualTo(4);
    }

    private UUID comunidadeDe(UUID post) {
        return jdbc.queryForObject("SELECT comunidade_id FROM post WHERE id = ?", UUID.class, post);
    }
}
