package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.readrace.api.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Chat do clube - /api/clubes/{clubeId}/mensagens")
class MensagemClubeControllerIT {

    private static final String CLUBE =
            "50000000-0000-0000-0000-000000000003";

    private static final String CLUBE_NAO_MEMBRO =
            "50000000-0000-0000-0000-000000000004";

    private static final String USUARIO_ATUAL =
            "00000000-0000-0000-0000-000000000001";

    private static final String URL =
            "/api/clubes/" + CLUBE + "/mensagens";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deve_listar_mensagens_do_clube() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.mensagens.length()")
                .isEqualTo(2);
    }

    @Test
    void deve_listar_mensagens_da_mais_antiga_para_a_mais_nova() {
        List<String> datas =
                JsonPath.read(corpo(URL), "$.mensagens[*].enviadaEm");

        assertThat(datas).isSorted();
    }

    @Test
    void deve_devolver_autor_da_mensagem() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.mensagens[0].autor.id")
                .isEqualTo("00000000-0000-0000-0000-000000000002");

        assertThat(mvc.get().uri(URL))
                .bodyJson()
                .extractingPath("$.mensagens[0].autor.nome")
                .isEqualTo("Ana Silva");
    }

    @Test
    void deve_marcar_minha_quando_mensagem_for_do_usuario_atual() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.mensagens[1].minha")
                .isEqualTo(true);
    }

    @Test
    void deve_buscar_apenas_mensagens_posteriores_ao_depois() {
        String ultimaData =
                JsonPath.read(
                        corpo(URL),
                        "$.mensagens[1].enviadaEm");

        assertThat(
                        mvc.get()
                                .uri(
                                        "/api/clubes/{clubeId}/mensagens?depois={depois}",
                                        CLUBE,
                                        OffsetDateTime.parse(ultimaData)))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.mensagens.length()")
                .isEqualTo(0);
    }

    @Test
    void deve_enviar_mensagem_e_persistir_no_banco() {
        String texto = "Mensagem criada pelo teste de integração";

        assertThat(
                        mvc.post()
                                .uri(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "texto": "Mensagem criada pelo teste de integração"
                                        }
                                        """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.texto")
                .isEqualTo(texto);

        Integer quantidade =
                jdbc.queryForObject(
                        """
                        SELECT count(*)
                        FROM mensagem_clube
                        WHERE clube_id = ?
                          AND autor_id = ?
                          AND texto = ?
                        """,
                        Integer.class,
                        UUID.fromString(CLUBE),
                        UUID.fromString(USUARIO_ATUAL),
                        texto);

        assertThat(quantidade).isEqualTo(1);
    }

    @Test
    @Transactional
    void deve_marcar_como_minha_a_mensagem_enviada() {
        assertThat(
                        mvc.post()
                                .uri(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "texto": "Minha mensagem"
                                        }
                                        """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.minha")
                .isEqualTo(true);
    }

    @Test
    void deve_rejeitar_mensagem_vazia() {
        assertThat(
                        mvc.post()
                                .uri(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "texto": "   "
                                        }
                                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("MENSAGEM_INVALIDA");
    }

    @Test
    void deve_rejeitar_mensagem_com_mais_de_mil_caracteres() {
        String texto = "a".repeat(1001);

        String json = """
                {
                  "texto": "%s"
                }
                """.formatted(texto);

        assertThat(
                        mvc.post()
                                .uri(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("MENSAGEM_INVALIDA");
    }

    @Test
    void deve_impedir_nao_membro_de_ler_chat() {
        assertThat(
                        mvc.get()
                                .uri(
                                        "/api/clubes/{clubeId}/mensagens",
                                        CLUBE_NAO_MEMBRO))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("SO_MEMBRO_NO_CHAT");
    }

    @Test
    void deve_impedir_nao_membro_de_enviar_mensagem() {
        assertThat(
                        mvc.post()
                                .uri(
                                        "/api/clubes/{clubeId}/mensagens",
                                        CLUBE_NAO_MEMBRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "texto": "Não deveria enviar"
                                        }
                                        """))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("SO_MEMBRO_NO_CHAT");
    }

    @Test
    void deve_devolver_404_quando_clube_nao_existir() {
        assertThat(
                        mvc.get()
                                .uri(
                                        "/api/clubes/{clubeId}/mensagens",
                                        UUID.randomUUID()))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("CLUBE_NAO_ENCONTRADO");
    }

    @Test
    @Transactional
    void deve_limitar_a_listagem_inicial_as_cem_mensagens_mais_recentes() {
        UUID clubeId = UUID.fromString(CLUBE);
        UUID autorId = UUID.fromString(USUARIO_ATUAL);

        for (int i = 0; i < 105; i++) {
            jdbc.update(
                    """
                    INSERT INTO mensagem_clube
                        (id, clube_id, autor_id, texto, enviada_em)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    UUID.randomUUID(),
                    clubeId,
                    autorId,
                    "Mensagem limite " + i,
                    OffsetDateTime.now().plusSeconds(i));
        }

        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.mensagens.length()")
                .isEqualTo(100);
    }

    private String corpo(String url) {
        try {
            return mvc.get()
                    .uri(url)
                    .exchange()
                    .getResponse()
                    .getContentAsString();
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}