package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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

import com.jayway.jsonpath.JsonPath;
import com.readrace.api.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GET /api/clubes/{clubeId}/posts")
class ForumClubeControllerIT {
    // Seed (V4): 6 posts por clube e 6 por comunidade. No clube 1 o post p é do usuário p, e
    // p=1 é o mais recente. As curtidas do post p do clube 1 são ((1 + p) % 7) + 1.
    private static final String CLUBE = "50000000-0000-0000-0000-000000000001";
    private static final String COMUNIDADE = "60000000-0000-0000-0000-000000000001";
    private static final String URL = "/api/clubes/" + CLUBE + "/posts";

    @Autowired private MockMvcTester mvc;

    @Autowired private JdbcTemplate jdbc;

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
                .isEqualTo("https://covers.openlibrary.org/b/isbn/9788535910663-L.jpg");
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
