package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

    private static final UUID EU =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final UUID OUTRO =
            UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static final UUID TERCEIRO =
            UUID.fromString("00000000-0000-0000-0000-000000000003");

    private static final UUID CLUBE =
            UUID.fromString("50000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper mapper;

    // ---------------------------------------------------------
    // Metodos auxiliares
    // ---------------------------------------------------------

    private UUID postDeComunidade(boolean sigo) {
        UUID comunidade = UUID.randomUUID();

        jdbc.update(
                "INSERT INTO comunidade (id, nome) "
                        + "VALUES (?, 'Comunidade de teste')",
                comunidade);

        if (sigo) {
            jdbc.update(
                    "INSERT INTO membro_comunidade "
                            + "(id, comunidade_id, usuario_id, cargo_comunidade) "
                            + "VALUES (?, ?, ?, 'membro')",
                    UUID.randomUUID(),
                    comunidade,
                    EU);
        }

        UUID post = UUID.randomUUID();

        jdbc.update(
                "INSERT INTO post "
                        + "(id, autor_id, comunidade_id, conteudo) "
                        + "VALUES (?, ?, ?, 'Post')",
                post,
                OUTRO,
                comunidade);

        return post;
    }

    private UUID postDeClube() {
        UUID post = UUID.randomUUID();

        jdbc.update(
                "INSERT INTO post "
                        + "(id, autor_id, clube_id, conteudo) "
                        + "VALUES (?, ?, ?, 'Post do clube')",
                post,
                OUTRO,
                CLUBE);

        return post;
    }

    private UUID postDeLivro() {
        UUID livroId = jdbc.queryForObject(
                "SELECT id FROM livro ORDER BY id LIMIT 1",
                UUID.class);

        UUID post = UUID.randomUUID();

        jdbc.update(
                "INSERT INTO post "
                        + "(id, autor_id, livro_id, conteudo) "
                        + "VALUES (?, ?, ?, 'Post do livro')",
                post,
                OUTRO,
                livroId);

        return post;
    }

    private void removerVinculoDoClube() {
        jdbc.update(
                "DELETE FROM membro_clube "
                        + "WHERE clube_id = ? AND usuario_id = ?",
                CLUBE,
                EU);

        entityManager.clear();
    }

    private void garantirMembroDoClube() {
        boolean membro = Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS ("
                                + "SELECT 1 FROM membro_clube "
                                + "WHERE clube_id = ? AND usuario_id = ?)",
                        Boolean.class,
                        CLUBE,
                        EU));

        if (!membro) {
            jdbc.update(
                    "INSERT INTO membro_clube "
                            + "(id, clube_id, usuario_id, pontos, cargo_clube) "
                            + "SELECT ?, ?, ?, 0, cargo_clube "
                            + "FROM membro_clube "
                            + "WHERE clube_id = ? "
                            + "LIMIT 1",
                    UUID.randomUUID(),
                    CLUBE,
                    EU,
                    CLUBE);
        }

        entityManager.clear();
    }

    private UUID comentarioDireto(
            UUID postId,
            UUID autorId,
            String texto,
            String atras) {

        UUID id = UUID.randomUUID();

        jdbc.update(
                "INSERT INTO post "
                        + "(id, autor_id, post_pai_id, conteudo, criado_em) "
                        + "VALUES (?, ?, ?, ?, now() - CAST(? AS interval))",
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
                "SELECT count(*) FROM post WHERE post_pai_id = ?",
                Integer.class,
                postId);
    }

    private UUID comunidadeDe(UUID post) {
        return jdbc.queryForObject(
                "SELECT comunidade_id FROM post WHERE id = ?",
                UUID.class,
                post);
    }

    // ---------------------------------------------------------
    // Testes existentes: comunidade
    // ---------------------------------------------------------

    @Test
    void lista_do_mais_antigo_para_o_mais_novo_com_autor() {
        UUID post = postDeComunidade(true);

        UUID novo = comentarioDireto(
                post, TERCEIRO, "Segundo", "1 hour");

        UUID antigo = comentarioDireto(
                post, OUTRO, "Primeiro", "2 hours");

        MvcTestResult resposta =
                mvc.get().uri(url(post)).exchange();

        assertThat(resposta).hasStatusOk();

        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.comentarios[*].id")
                .isEqualTo(List.of(
                        antigo.toString(),
                        novo.toString()));

        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.comentarios[0].texto")
                .isEqualTo("Primeiro");

        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.comentarios[0].autor.id")
                .isEqualTo(OUTRO.toString());
    }

    @Test
    void nao_lista_comentario_excluido() {
        UUID post = postDeComunidade(true);

        UUID excluido = comentarioDireto(
                post, OUTRO, "Apagado", "1 hour");

        jdbc.update(
                "UPDATE post SET excluido_em = now() WHERE id = ?",
                excluido);

        assertThat(mvc.get().uri(url(post)))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.comentarios")
                .asArray()
                .isEmpty();
    }

    @Test
    void informa_podeComentar_e_deixa_quem_nao_segue_ler() {
        for (boolean sigo : new boolean[] {true, false}) {
            UUID post = postDeComunidade(sigo);

            comentarioDireto(
                    post,
                    OUTRO,
                    "Visivel para todos",
                    "1 hour");

            MvcTestResult resposta =
                    mvc.get().uri(url(post)).exchange();

            assertThat(resposta).hasStatusOk();

            assertThat(resposta)
                    .bodyJson()
                    .extractingPath("$.podeComentar")
                    .isEqualTo(sigo);

            assertThat(resposta)
                    .bodyJson()
                    .extractingPath("$.comentarios")
                    .asArray()
                    .hasSize(1);
        }
    }

    @Test
    void comenta_como_post_com_pai_e_sem_escopo_proprio()
            throws Exception {

        UUID post = postDeComunidade(true);

        MvcTestResult resposta =
                comentar(post, "{\"texto\":\"Meu comentario\"}");

        assertThat(resposta).hasStatus(HttpStatus.CREATED);

        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.texto")
                .isEqualTo("Meu comentario");

        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.autor.id")
                .isEqualTo(EU.toString());

        entityManager.flush();

        UUID id = UUID.fromString(
                JsonPath.read(
                        resposta.getResponse().getContentAsString(),
                        "$.id"));

        Map<String, Object> linha =
                jdbc.queryForMap(
                        "SELECT autor_id, post_pai_id, clube_id, "
                                + "comunidade_id, livro_id, conteudo "
                                + "FROM post WHERE id = ?",
                        id);

        assertThat(linha)
                .containsEntry("autor_id", EU)
                .containsEntry("post_pai_id", post)
                .containsEntry("clube_id", null)
                .containsEntry("comunidade_id", null)
                .containsEntry("livro_id", null)
                .containsEntry("conteudo", "Meu comentario");
    }

    @Test
    void comentario_novo_aparece_na_lista_e_no_contador() {
        UUID post = postDeComunidade(true);

        comentarioDireto(post, OUTRO, "Antigo", "1 hour");

        UUID comunidade = comunidadeDe(post);

        assertThat(comentar(post, "{\"texto\":\"Novo\"}"))
                .hasStatus(HttpStatus.CREATED);

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
                .extractingPath("$.code")
                .isEqualTo("SO_QUEM_SEGUE_COMENTA");

        entityManager.flush();

        assertThat(totalComentarios(post)).isZero();
    }

    @Test
    void valida_o_texto() {
        for (String json : corposInvalidos()) {
            UUID post = postDeComunidade(true);

            assertThat(comentar(post, json))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("COMENTARIO_INVALIDO");

            entityManager.flush();

            assertThat(totalComentarios(post)).isZero();
        }
    }

    private static List<String> corposInvalidos() {
        return List.of(
                "{}",
                "{\"texto\":null}",
                "{\"texto\":\"\"}",
                "{\"texto\":\" \\t\\r\\n \"}",
                "{\"texto\":\"" + "a".repeat(1001) + "\"}");
    }

    @Test
    void aceita_os_limites_do_texto() throws Exception {
        for (int tamanho : new int[] {1, 1000}) {
            UUID post = postDeComunidade(true);
            String texto = "a".repeat(tamanho);

            assertThat(
                    comentar(
                            post,
                            mapper.writeValueAsString(
                                    Map.of("texto", texto))))
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson()
                    .extractingPath("$.texto")
                    .isEqualTo(texto);
        }
    }

    @Test
    void responde_404_para_post_que_nao_aceita_comentario() {
        for (String caso : postsQueNaoAceitamComentario()) {
            UUID post = postQueNaoAceita(caso);

            assertThat(mvc.get().uri(url(post)))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("POST_NAO_ENCONTRADO");

            assertThat(comentar(post, "{\"texto\":\"Oi\"}"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("POST_NAO_ENCONTRADO");
        }
    }

    private static List<String> postsQueNaoAceitamComentario() {
        return List.of(
                "inexistente",
                "excluido",
                "resposta abaixo de comentario excluido",
                "de comunidade excluida");
    }

    private UUID postQueNaoAceita(String caso) {
        return switch (caso) {
            case "inexistente" -> UUID.randomUUID();

            case "excluido" -> {
                UUID post = postDeComunidade(true);

                jdbc.update(
                        "UPDATE post SET excluido_em = now() WHERE id = ?",
                        post);

                yield post;
            }

            case "resposta abaixo de comentario excluido" -> {
                UUID pai = comentarioDireto(
                        postDeComunidade(true),
                        OUTRO,
                        "Pai",
                        "2 hours");

                UUID resposta = comentarioDireto(
                        pai,
                        TERCEIRO,
                        "Resposta",
                        "1 hour");

                jdbc.update(
                        "UPDATE post SET excluido_em = now() WHERE id = ?",
                        pai);

                yield resposta;
            }

            case "de comunidade excluida" -> {
                UUID post = postDeComunidade(true);

                jdbc.update(
                        "UPDATE comunidade SET excluido_em = now() "
                                + "WHERE id = "
                                + "(SELECT comunidade_id FROM post WHERE id = ?)",
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
    void responde_a_um_comentario_e_lista_thread_em_arvore()
            throws Exception {

        UUID post = postDeComunidade(true);

        UUID primeiro = comentarioDireto(
                post, OUTRO, "Primeiro", "3 hours");

        UUID segundo = comentarioDireto(
                post, TERCEIRO, "Segundo", "2 hours");

        UUID respostaAntiga = comentarioDireto(
                primeiro, TERCEIRO, "Resposta antiga", "150 minutes");

        MvcTestResult criada =
                comentar(primeiro, "{\"texto\":\"Minha resposta\"}");

        assertThat(criada).hasStatus(HttpStatus.CREATED);

        entityManager.flush();

        UUID minha = UUID.fromString(
                JsonPath.read(
                        criada.getResponse().getContentAsString(),
                        "$.id"));

        assertThat(
                jdbc.queryForObject(
                        "SELECT post_pai_id FROM post WHERE id = ?",
                        UUID.class,
                        minha))
                .isEqualTo(primeiro);

        UUID netinha = comentarioDireto(
                minha, OUTRO, "Resposta da resposta", "0 minutes");

        MvcTestResult thread =
                mvc.get().uri(url(post)).exchange();

        assertThat(thread).hasStatusOk();

        assertThat(thread)
                .bodyJson()
                .extractingPath("$.comentarios[*].id")
                .isEqualTo(List.of(
                        primeiro.toString(),
                        segundo.toString()));

        assertThat(thread)
                .bodyJson()
                .extractingPath("$.comentarios[0].respostas[*].id")
                .isEqualTo(List.of(
                        respostaAntiga.toString(),
                        minha.toString()));

        assertThat(thread)
                .bodyJson()
                .extractingPath(
                        "$.comentarios[0].respostas[1].respostas[*].id")
                .isEqualTo(List.of(netinha.toString()));
    }

    @Test
    void comentario_excluido_esconde_respostas() {
        UUID post = postDeComunidade(true);

        UUID pai = comentarioDireto(
                post, OUTRO, "Pai", "2 hours");

        comentarioDireto(
                pai, TERCEIRO, "Resposta", "1 hour");

        jdbc.update(
                "UPDATE post SET excluido_em = now() WHERE id = ?",
                pai);

        assertThat(mvc.get().uri(url(post)))
                .bodyJson()
                .extractingPath("$.comentarios")
                .asArray()
                .isEmpty();
    }

    @Test
    void quem_nao_segue_nao_responde() {
        UUID post = postDeComunidade(false);

        UUID comentario = comentarioDireto(
                post, OUTRO, "Pai", "1 hour");

        assertThat(comentar(
                comentario,
                "{\"texto\":\"Resposta\"}"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("SO_QUEM_SEGUE_COMENTA");

        entityManager.flush();

        assertThat(totalComentarios(comentario)).isZero();
    }

    @Test
    void comentario_tem_curtidas() {
        UUID post = postDeComunidade(true);

        UUID comentario = comentarioDireto(
                post, OUTRO, "Curta", "1 hour");

        jdbc.update(
                "INSERT INTO curtida (id, post_id, usuario_id) "
                        + "VALUES (?, ?, ?)",
                UUID.randomUUID(),
                comentario,
                TERCEIRO);

        assertThat(
                mvc.post()
                        .uri("/api/posts/" + comentario + "/curtida"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalCurtidas")
                .isEqualTo(2);

        entityManager.flush();

        assertThat(mvc.get().uri(url(post)))
                .bodyJson()
                .extractingPath(
                        "$.comentarios[0].totalCurtidas")
                .isEqualTo(2);
    }

    @Test
    void contador_soma_respostas_de_todos_os_niveis() {
        UUID post = postDeComunidade(true);

        UUID pai = comentarioDireto(
                post, OUTRO, "Pai", "3 hours");

        UUID filha = comentarioDireto(
                pai, TERCEIRO, "Filha", "2 hours");

        comentarioDireto(
                filha, OUTRO, "Neta", "1 hour");

        comentarioDireto(
                post, TERCEIRO, "Outro", "1 hour");

        assertThat(mvc.get().uri(
                "/api/comunidades/" + comunidadeDe(post)))
                .bodyJson()
                .extractingPath("$.posts[0].totalComentarios")
                .isEqualTo(4);
    }

    // ---------------------------------------------------------
    // Task #236 - Forum do clube
    // ---------------------------------------------------------

    @Test
    void permite_ler_comentarios_do_clube() {
        UUID post = postDeClube();

        comentarioDireto(
                post, OUTRO, "Comentario do forum", "1 hour");

        MvcTestResult resposta =
                mvc.get().uri(url(post)).exchange();

        assertThat(resposta).hasStatusOk();

        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.comentarios")
                .asArray()
                .hasSize(1);
    }

    @Test
    void nao_membro_pode_ler_mas_nao_comentar_no_clube() {
        removerVinculoDoClube();

        UUID post = postDeClube();

        MvcTestResult leitura =
                mvc.get().uri(url(post)).exchange();

        assertThat(leitura).hasStatusOk();

        assertThat(leitura)
                .bodyJson()
                .extractingPath("$.podeComentar")
                .isEqualTo(false);

        assertThat(comentar(
                post,
                "{\"texto\":\"Tentativa de comentario\"}"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("SO_MEMBRO_POSTA");

        entityManager.flush();

        assertThat(totalComentarios(post)).isZero();
    }

    @Test
    void membro_pode_comentar_no_clube() {
        garantirMembroDoClube();

        UUID post = postDeClube();

        assertThat(mvc.get().uri(url(post)))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.podeComentar")
                .isEqualTo(true);

        MvcTestResult resposta =
                comentar(
                        post,
                        "{\"texto\":\"Comentario do membro\"}");

        assertThat(resposta).hasStatus(HttpStatus.CREATED);

        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.texto")
                .isEqualTo("Comentario do membro");

        entityManager.flush();

        assertThat(totalComentarios(post)).isEqualTo(1);
    }

    @Test
    void membro_pode_responder_comentario_do_clube() {
        garantirMembroDoClube();

        UUID post = postDeClube();

        UUID comentario = comentarioDireto(
                post, OUTRO, "Comentario original", "1 hour");

        MvcTestResult resposta =
                comentar(
                        comentario,
                        "{\"texto\":\"Resposta do membro\"}");

        assertThat(resposta).hasStatus(HttpStatus.CREATED);

        entityManager.flush();

        assertThat(totalComentarios(comentario)).isEqualTo(1);
    }

    // ---------------------------------------------------------
    // Task #236 - Detalhe do livro
    // ---------------------------------------------------------

    @Test
    void permite_ler_comentarios_do_livro() {
        UUID post = postDeLivro();

        MvcTestResult resposta =
                mvc.get().uri(url(post)).exchange();

        assertThat(resposta).hasStatusOk();

        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.podeComentar")
                .isEqualTo(true);
    }

    @Test
    void permite_comentar_em_post_do_livro() {
        UUID post = postDeLivro();

        MvcTestResult resposta =
                comentar(
                        post,
                        "{\"texto\":\"Gostei muito deste livro\"}");

        assertThat(resposta).hasStatus(HttpStatus.CREATED);

        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.texto")
                .isEqualTo("Gostei muito deste livro");

        entityManager.flush();

        assertThat(totalComentarios(post)).isEqualTo(1);
    }

    @Test
    void permite_responder_comentario_do_livro() {
        UUID post = postDeLivro();

        UUID comentario = comentarioDireto(
                post, OUTRO, "Comentario original", "1 hour");

        MvcTestResult resposta =
                comentar(
                        comentario,
                        "{\"texto\":\"Minha resposta\"}");

        assertThat(resposta).hasStatus(HttpStatus.CREATED);

        entityManager.flush();

        assertThat(totalComentarios(comentario)).isEqualTo(1);
    }
}