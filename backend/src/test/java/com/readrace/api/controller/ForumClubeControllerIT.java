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
import org.junit.jupiter.params.provider.CsvSource;
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
@DisplayName("GET e POST /api/clubes/{clubeId}/posts")
class ForumClubeControllerIT {
    // Seed (V4): 6 posts por clube e 6 por comunidade. No clube 1 o post p é do usuário p, e
    // p=1 é o mais recente. As curtidas do post p do clube 1 são ((1 + p) % 7) + 1.
    private static final String CLUBE = "50000000-0000-0000-0000-000000000001";
    private static final String COMUNIDADE = "60000000-0000-0000-0000-000000000001";
    private static final String URL = "/api/clubes/" + CLUBE + "/posts";
    private static final UUID USUARIO_ATUAL =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired private MockMvcTester mvc;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private ObjectMapper mapper;

    @ParameterizedTest
    @ValueSource(strings = {"membro", "administrador"})
    @Transactional
    void deve_publicar_no_clube_sem_efeitos_em_outros_escopos_ou_gamificacao(String cargo)
            throws Exception {
        jdbc.update(
                "UPDATE membro_clube SET cargo_clube = CAST(? AS cargo)"
                        + " WHERE clube_id = ? AND usuario_id = ?",
                cargo,
                UUID.fromString(CLUBE),
                USUARIO_ATUAL);
        // Datas antigas e distintas garantem que o topo não depende de empate no relógio.
        jdbc.update(
                "UPDATE post SET criado_em = TIMESTAMPTZ '2020-01-01 00:00:00+00'"
                        + " + autor.numero * INTERVAL '1 second' FROM"
                        + " (SELECT id, row_number() OVER (ORDER BY id) AS numero FROM post) autor"
                        + " WHERE post.id = autor.id AND post.clube_id = ?",
                UUID.fromString(CLUBE));
        String outroClube = "/api/clubes/50000000-0000-0000-0000-000000000002/posts";
        String detalheLivro = "/api/livros/30000000-0000-0000-0000-000000000001";
        Object outroAntes = jsonGet(outroClube);
        Object livroAntes = jsonGet(detalheLivro);
        var gamificacaoAntes = gamificacao();
        int postsAntes = totalPosts();
        String texto = " \t\u00A0 Leitura de hoje\r\ncontinua aqui. 😀\n ";

        MvcTestResult resposta = enviar(URL, mapper.writeValueAsString(Map.of("texto", texto)));

        assertThat(resposta).hasStatus(HttpStatus.CREATED);
        Map<String, Object> criado =
                JsonPath.read(resposta.getResponse().getContentAsString(), "$");
        assertThat(criado)
                .containsOnlyKeys(
                        "id", "autor", "publicadoEm", "texto", "totalCurtidas", "curtidoPorMim");
        assertThat(criado)
                .containsEntry("texto", texto)
                .containsEntry("totalCurtidas", 0)
                .containsEntry("curtidoPorMim", false);
        UUID postId = UUID.fromString((String) criado.get("id"));
        entityManager.flush();
        assertThat(totalPosts()).isEqualTo(postsAntes + 1);
        Map<String, Object> salvo = jdbc.queryForMap("SELECT * FROM post WHERE id = ?", postId);
        assertThat(salvo)
                .containsEntry("autor_id", USUARIO_ATUAL)
                .containsEntry("clube_id", UUID.fromString(CLUBE))
                .containsEntry("conteudo", texto)
                .containsEntry("livro_id", null)
                .containsEntry("comunidade_id", null)
                .containsEntry("post_pai_id", null)
                .containsEntry("excluido_em", null);
        assertThat(salvo.get("criado_em")).isNotNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM curtida WHERE post_id = ?",
                                Integer.class,
                                postId))
                .isZero();
        Map<String, Object> primeiro = JsonPath.read(corpo(), "$.posts[0]");
        assertThat(primeiro).isEqualTo(criado);
        assertThat(JsonPath.<String>read(corpo(), "$.posts[0].autor.id"))
                .isEqualTo(USUARIO_ATUAL.toString());
        assertThat(JsonPath.<List<String>>read(corpo(), "$.posts[*].publicadoEm"))
                .doesNotHaveDuplicates();
        assertThat(jsonGet(outroClube)).isEqualTo(outroAntes);
        assertThat(jsonGet(detalheLivro)).isEqualTo(livroAntes);

        String curtida = "/api/posts/" + postId + "/curtida";
        assertThat(mvc.post().uri(curtida))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("totalCurtidas", 1)
                .containsEntry("curtidoPorMim", true);
        assertThat(JsonPath.<Boolean>read(corpo(), "$.posts[0].curtidoPorMim")).isTrue();
        assertThat(mvc.delete().uri(curtida))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("totalCurtidas", 0)
                .containsEntry("curtidoPorMim", false);
        assertThat(JsonPath.<Map<String, Object>>read(corpo(), "$.posts[0]")).isEqualTo(criado);
        assertThat(gamificacao()).isEqualTo(gamificacaoAntes);
    }

    @Test
    @Transactional
    void nao_deve_publicar_sem_vinculo() {
        jdbc.update(
                "DELETE FROM membro_clube WHERE clube_id = ? AND usuario_id = ?",
                UUID.fromString(CLUBE),
                USUARIO_ATUAL);
        int antes = totalPosts();

        assertThat(enviar(URL, "{\"texto\":\"Leitura de hoje\"}"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("code", "SO_MEMBRO_POSTA")
                .containsEntry("message", "Somente membros do clube podem publicar no fórum.");

        entityManager.flush();
        assertThat(totalPosts()).isEqualTo(antes);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @Transactional
    void nao_deve_publicar_em_clube_inexistente_ou_excluido(boolean excluido) {
        UUID clubeId = excluido ? UUID.fromString(CLUBE) : UUID.randomUUID();
        if (excluido) {
            jdbc.update("UPDATE clube_do_livro SET excluido_em = now() WHERE id = ?", clubeId);
        }
        int antes = totalPosts();

        assertThat(enviar("/api/clubes/" + clubeId + "/posts", "{\"texto\":\"Leitura\"}"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("code", "CLUBE_NAO_ENCONTRADO")
                .containsEntry("message", "Clube não encontrado.");

        entityManager.flush();
        assertThat(totalPosts()).isEqualTo(antes);
        assertThat(mvc.get().uri("/api/clubes/{id}/posts", clubeId))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    @ParameterizedTest
    @MethodSource("corposInvalidos")
    @Transactional
    void deve_validar_texto_recebido_pelo_endpoint(String json) {
        int antes = totalPosts();

        assertThat(enviar(URL, json))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("code", "POST_INVALIDO")
                .containsEntry(
                        "message", "O post deve conter texto e ter no máximo 2.000 caracteres.");

        entityManager.flush();
        assertThat(totalPosts()).isEqualTo(antes);
    }

    static Stream<String> corposInvalidos() {
        return Stream.of(
                "{}",
                "{\"texto\":null}",
                "{\"texto\":\"\"}",
                "{\"texto\":\" \\t\\r\\n\u00A0 \"}",
                "{\"texto\":\"" + "a".repeat(2001) + "\"}");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2000})
    @Transactional
    void deve_aceitar_limites_de_texto_no_endpoint(int tamanho) throws Exception {
        String texto = "a".repeat(tamanho);

        MvcTestResult resposta = enviar(URL, mapper.writeValueAsString(Map.of("texto", texto)));

        assertThat(resposta)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.texto")
                .isEqualTo(texto);
        entityManager.flush();
        UUID id =
                UUID.fromString(JsonPath.read(resposta.getResponse().getContentAsString(), "$.id"));
        assertThat(jdbc.queryForObject("SELECT conteudo FROM post WHERE id = ?", String.class, id))
                .isEqualTo(texto);
    }

    @Test
    @Transactional
    void deve_manter_tratamento_de_json_malformado() {
        int antes = totalPosts();

        assertThat(enviar(URL, "{\"texto\":"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("MALFORMED_REQUEST");

        assertThat(totalPosts()).isEqualTo(antes);
    }

    private MvcTestResult enviar(String url, String json) {
        return mvc.post().uri(url).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private Object jsonGet(String url) throws Exception {
        MvcTestResult resposta = mvc.get().uri(url).exchange();
        assertThat(resposta).hasStatusOk();
        return JsonPath.read(resposta.getResponse().getContentAsString(), "$");
    }

    private int totalPosts() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM post", Integer.class);
    }

    private Map<String, List<Map<String, Object>>> gamificacao() {
        return Map.of(
                "usuarios",
                        jdbc.queryForList(
                                "SELECT id, xp_total, nivel, dias_consecutivos,"
                                        + " ultima_leitura_em FROM usuario ORDER BY id"),
                "membros", jdbc.queryForList("SELECT id, pontos FROM membro_clube ORDER BY id"),
                "conquistas", jdbc.queryForList("SELECT * FROM conquista_usuario ORDER BY id"),
                "desafios", jdbc.queryForList("SELECT * FROM progresso_desafio ORDER BY id"));
    }

    @ParameterizedTest(name = "cargo={0}, fórum vazio={1}, podeEscrever={2}")
    @CsvSource({
        "membro, false, true",
        "administrador, false, true",
        "sem_vinculo, false, false",
        "membro, true, true",
        "administrador, true, true",
        "sem_vinculo, true, false"
    })
    @Transactional
    void deve_informar_permissao_de_escrita_sem_restringir_leitura(
            String cargo, boolean forumVazio, boolean podeEscrever) {
        UUID clubeId = UUID.fromString(CLUBE);
        UUID usuarioId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        if (cargo.equals("sem_vinculo")) {
            assertThat(
                            jdbc.update(
                                    "DELETE FROM membro_clube WHERE clube_id = ? AND usuario_id = ?",
                                    clubeId,
                                    usuarioId))
                    .isEqualTo(1);
        } else {
            assertThat(
                            jdbc.update(
                                    "UPDATE membro_clube SET cargo_clube = CAST(? AS cargo)"
                                            + " WHERE clube_id = ? AND usuario_id = ?",
                                    cargo,
                                    clubeId,
                                    usuarioId))
                    .isEqualTo(1);
        }
        if (forumVazio) {
            jdbc.update("UPDATE post SET excluido_em = now() WHERE clube_id = ?", clubeId);
        }

        String resposta = corpo();
        assertThat(JsonPath.<Boolean>read(resposta, "$.podeEscrever")).isEqualTo(podeEscrever);
        assertThat(JsonPath.<String>read(resposta, "$.clube.id")).isEqualTo(CLUBE);
        assertThat(JsonPath.<List<?>>read(resposta, "$.posts")).hasSize(forumVazio ? 0 : 6);
    }

    @Test
    @DisplayName("o cabeçalho vem junto, para a tela não fazer uma segunda chamada")
    void deve_devolver_o_cabecalho_com_clube_e_livro_atual() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.clube.nome")
                .isEqualTo("Clube dos Clássicos Brasileiros");
        assertThat(mvc.get().uri(URL))
                .bodyJson()
                .extractingPath("$.clube.livroAtual.titulo")
                .isEqualTo("Dom Casmurro");
        assertThat(mvc.get().uri(URL))
                .bodyJson()
                .extractingPath("$.clube.livroAtual.autor")
                .isEqualTo("Machado de Assis");
        assertThat(mvc.get().uri(URL))
                .bodyJson()
                .extractingPath("$.clube.livroAtual.capaUrl")
                .isEqualTo("https://covers.openlibrary.org/b/id/647501-L.jpg");
    }

    @Test
    void deve_listar_os_posts_do_mais_recente_para_o_mais_antigo() {
        List<String> publicados = JsonPath.read(corpo(), "$.posts[*].publicadoEm");

        assertThat(publicados).hasSize(6).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    @DisplayName("autor completo: id, nome, avatar e a sequência que alimenta o badge de chama")
    void deve_devolver_o_autor_de_cada_post() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.posts[0].autor.id")
                .isEqualTo("00000000-0000-0000-0000-000000000001");
        assertThat(mvc.get().uri(URL))
                .bodyJson()
                .extractingPath("$.posts[0].autor.nome")
                .isEqualTo("Daniel Ribeiro");
        assertThat(mvc.get().uri(URL))
                .bodyJson()
                .extractingPath("$.posts[0].autor.avatarUrl")
                .isEqualTo("https://i.pravatar.cc/300?img=12");
        assertThat(mvc.get().uri(URL))
                .bodyJson()
                .extractingPath("$.posts[0].autor.sequenciaDias")
                .isEqualTo(
                        jdbc.queryForObject(
                                "SELECT dias_consecutivos FROM usuario WHERE id ="
                                        + " '00000000-0000-0000-0000-000000000001'",
                                Integer.class));
    }

    @Test
    @DisplayName("curtidas são contagem de exibição — ((1 + p) % 7) + 1 no seed")
    void deve_contar_as_curtidas_de_cada_post() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.posts[*].totalCurtidas")
                .asList()
                .containsExactly(3, 4, 5, 6, 7, 1);
    }

    @Test
    @DisplayName("curtidoPorMim reflete a curtida do usuário atual (curtiu tudo no seed) (#101)")
    void deve_marcar_curtidoPorMim_para_o_usuario_atual() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.posts[*].curtidoPorMim")
                .asList()
                .containsOnly(true);
    }

    @Test
    @DisplayName("o backend não trunca: o corte é decisão do PostCard")
    void deve_devolver_o_texto_inteiro() {
        String esperado =
                jdbc.queryForObject(
                        "SELECT conteudo FROM post WHERE clube_id = ? AND post_pai_id IS NULL"
                                + " AND excluido_em IS NULL ORDER BY criado_em DESC LIMIT 1",
                        String.class,
                        UUID.fromString(CLUBE));

        assertThat(JsonPath.<String>read(corpo(), "$.posts[0].texto"))
                .isEqualTo(esperado)
                .doesNotEndWith("...");
    }

    @Test
    @DisplayName("post de comunidade nunca aparece no fórum do clube")
    void nao_deve_devolver_post_de_comunidade() {
        List<String> idsDaComunidade =
                jdbc.queryForList(
                        "SELECT id::text FROM post WHERE comunidade_id = ?",
                        String.class,
                        UUID.fromString(COMUNIDADE));
        List<String> idsNaResposta = JsonPath.read(corpo(), "$.posts[*].id");

        assertThat(idsDaComunidade).isNotEmpty();
        assertThat(idsNaResposta).doesNotContainAnyElementsOf(idsDaComunidade);
    }

    @Test
    @Transactional
    @DisplayName("comentário é post com pai e não entra na listagem")
    void nao_deve_devolver_comentario() {
        UUID comentario = UUID.fromString("39000000-0000-0000-0000-000000000001");
        String pai = jdbc.queryForObject("SELECT md5('post-clube-1-1')::uuid::text", String.class);
        jdbc.update(
                "INSERT INTO post (id, autor_id, post_pai_id, conteudo)"
                        + " VALUES (?, '00000000-0000-0000-0000-000000000001', ?, 'Comentário')",
                comentario,
                UUID.fromString(pai));

        List<String> ids = JsonPath.read(corpo(), "$.posts[*].id");

        assertThat(ids).hasSize(6).doesNotContain(comentario.toString());
    }

    @Test
    @Transactional
    void nao_deve_devolver_post_excluido() {
        jdbc.update("UPDATE post SET excluido_em = now() WHERE id = md5('post-clube-1-1')::uuid");

        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.posts.length()")
                .isEqualTo(5);
    }

    @Test
    @Transactional
    @DisplayName("clube sem posts devolve lista vazia, com o cabeçalho completo")
    void deve_devolver_lista_vazia_quando_o_clube_nao_tem_posts() {
        UUID clubeVazio = UUID.fromString("59000000-0000-0000-0000-000000000002");
        jdbc.update(
                "INSERT INTO clube_do_livro (id, livro_id, nome) VALUES"
                        + " (?, '30000000-0000-0000-0000-000000000001', 'Clube sem posts')",
                clubeVazio);

        assertThat(mvc.get().uri("/api/clubes/{id}/posts", clubeVazio))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.clube.livroAtual.titulo")
                .isEqualTo("Dom Casmurro");
        assertThat(mvc.get().uri("/api/clubes/{id}/posts", clubeVazio))
                .bodyJson()
                .extractingPath("$.posts.length()")
                .isEqualTo(0);
    }

    @Test
    void deve_devolver_404_no_formato_padrao_quando_o_clube_nao_existir() {
        assertThat(mvc.get().uri("/api/clubes/{id}/posts", UUID.randomUUID()))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("id de comunidade responde 404: este fórum é do clube")
    void deve_devolver_404_para_id_de_comunidade() {
        assertThat(mvc.get().uri("/api/clubes/{id}/posts", COMUNIDADE))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    private String corpo() {
        try {
            return mvc.get().uri(URL).exchange().getResponse().getContentAsString();
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
