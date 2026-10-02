package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.jayway.jsonpath.JsonPath;
import com.readrace.api.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GET /api/biblioteca")
class BibliotecaControllerIT {
    // Ids do seed (V4): o usuário fixo tem 12 itens — 4 lendo, 5 lidos, 3 desejo, 3 favoritos.
    private static final String LIVRO_DOM_CASMURRO = "30000000-0000-0000-0000-000000000001";
    private static final String LIVRO_LIDO_MAIS_RECENTE = "30000000-0000-0000-0000-000000000007";

    @Autowired private MockMvcTester mvc;

    @Test
    void deve_devolver_exatamente_as_quatro_listas_e_nenhuma_recomendacao() {
        assertThat(mvc.get().uri("/api/biblioteca"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsOnlyKeys("favoritos", "lendo", "desejo", "lidos");
    }

    @Test
    void cada_lista_deve_ser_uma_pagina_com_itens_e_cursor() {
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.lendo")
                .asMap()
                .containsOnlyKeys("itens", "proximoCursor");
    }

    @Test
    void deve_devolver_os_favoritos_do_usuario_fixo_sem_receber_id_do_cliente() {
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.favoritos.itens.length()")
                .isEqualTo(3);
    }

    @Test
    void deve_repartir_os_itens_por_estado_de_leitura() {
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.lendo.itens.length()")
                .isEqualTo(4);
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.desejo.itens.length()")
                .isEqualTo(3);
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.lidos.itens.length()")
                .isEqualTo(5);
    }

    @Test
    void nao_deve_devolver_cursor_quando_a_lista_cabe_na_primeira_pagina() {
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.lidos.proximoCursor")
                .isNull();
    }

    @Test
    void deve_por_primeiro_o_livro_com_registro_de_leitura_mais_recente() {
        // Dom Casmurro foi adicionado há 30 dias, mas tem registro de leitura de ontem.
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.lendo.itens[0].livroId")
                .isEqualTo(LIVRO_DOM_CASMURRO);
    }

    @Test
    void deve_ordenar_pela_data_de_adicao_quando_nao_houver_registro_de_leitura() {
        // Nenhum item lido tem registro; o adicionado há 50 dias vem antes dos de 60, 75, 90 e 120.
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.lidos.itens[0].livroId")
                .isEqualTo(LIVRO_LIDO_MAIS_RECENTE);
    }

    @Test
    void deve_percorrer_a_lista_inteira_seguindo_o_cursor_sem_repetir_nem_pular() {
        List<String> semPaginacao =
                JsonPath.read(corpo("/api/biblioteca/lidos"), "$.itens[*].livroId");
        List<String> paginados = new ArrayList<>();
        String cursor = null;
        int paginas = 0;

        do {
            String url =
                    "/api/biblioteca/lidos?limite=2" + (cursor == null ? "" : "&cursor=" + cursor);
            String pagina = corpo(url);
            paginados.addAll(JsonPath.read(pagina, "$.itens[*].livroId"));
            cursor = JsonPath.read(pagina, "$.proximoCursor");
            paginas++;
        } while (cursor != null);

        assertThat(paginas).isEqualTo(3);
        assertThat(paginados).containsExactlyElementsOf(semPaginacao).doesNotHaveDuplicates();
    }

    @Test
    void deve_limitar_a_primeira_pagina_de_todas_as_listas_e_devolver_cursor_onde_sobrar() {
        assertThat(mvc.get().uri("/api/biblioteca?limite=2"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.lendo.itens.length()")
                .isEqualTo(2);
        assertThat(mvc.get().uri("/api/biblioteca?limite=2"))
                .bodyJson()
                .extractingPath("$.lendo.proximoCursor")
                .asString()
                .isNotBlank();
        assertThat(mvc.get().uri("/api/biblioteca?limite=3"))
                .bodyJson()
                .extractingPath("$.favoritos.proximoCursor")
                .isNull();
    }

    @Test
    void favorito_deve_aparecer_tambem_na_lista_do_seu_estado() {
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.lendo.itens[*].livroId")
                .asList()
                .contains(LIVRO_DOM_CASMURRO);
        assertThat(mvc.get().uri("/api/biblioteca/favoritos"))
                .bodyJson()
                .extractingPath("$.itens[*].livroId")
                .asList()
                .contains(LIVRO_DOM_CASMURRO);
    }

    @Test
    void deve_devolver_titulo_autor_e_capa_de_cada_livro() {
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.lendo.itens[0]")
                .asMap()
                .containsEntry("titulo", "Dom Casmurro")
                .containsEntry("autor", "Machado de Assis")
                .containsKeys("capaUrl", "livroId");
    }

    @Test
    void deve_ignorar_o_id_que_o_cliente_tentar_passar_na_query() {
        assertThat(mvc.get().uri("/api/biblioteca?userId=99999999-9999-9999-9999-999999999999"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.favoritos.itens.length()")
                .isEqualTo(3);
    }

    @Test
    @Sql(
            statements =
                    """
                    UPDATE item_biblioteca
                    SET status_leitura = 'lido'
                    WHERE usuario_id = '00000000-0000-0000-0000-000000000001'
                      AND status_leitura = 'desejo';
                    """)
    @Sql(
            statements =
                    """
                    UPDATE item_biblioteca
                    SET status_leitura = 'desejo'
                    WHERE id IN (
                        '40000000-0000-0000-0000-000000000005',
                        '40000000-0000-0000-0000-000000000008',
                        '40000000-0000-0000-0000-000000000011'
                    );
                    """,
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void deve_devolver_pagina_vazia_e_nao_erro_quando_nao_houver_item_no_estado() {
        assertThat(mvc.get().uri("/api/biblioteca"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.desejo")
                .asMap()
                .containsEntry("itens", List.of())
                .containsEntry("proximoCursor", null);
        assertThat(mvc.get().uri("/api/biblioteca/lidos"))
                .bodyJson()
                .extractingPath("$.itens.length()")
                .isEqualTo(8);
    }

    @Test
    void deve_devolver_404_no_formato_padrao_para_lista_que_nao_existe() {
        assertThat(mvc.get().uri("/api/biblioteca/recomendacoes"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void deve_devolver_400_parametro_invalido_para_cursor_que_a_api_nao_gerou() {
        assertThat(mvc.get().uri("/api/biblioteca/lidos?cursor=nao-e-um-cursor"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("PARAMETRO_INVALIDO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "51", "-1"})
    void deve_devolver_400_parametro_invalido_para_limite_fora_da_faixa(String limite) {
        assertThat(mvc.get().uri("/api/biblioteca?limite=" + limite))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("PARAMETRO_INVALIDO");
    }

    @Test
    void deve_devolver_400_malformed_request_para_limite_que_nao_e_numero() {
        assertThat(mvc.get().uri("/api/biblioteca?limite=vinte"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    void deve_recusar_o_verbo_errado_no_formato_padrao_de_erro() {
        assertThat(mvc.post().uri("/api/biblioteca"))
                .hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("METHOD_NOT_ALLOWED");
    }

    private String corpo(String url) {
        try {
            return mvc.get().uri(url).exchange().getResponse().getContentAsString();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
