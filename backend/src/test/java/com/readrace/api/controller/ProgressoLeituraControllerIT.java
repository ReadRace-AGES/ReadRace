package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.readrace.api.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("POST /api/livros/{livroId}/progresso")
@Sql(
        statements =
                """
                UPDATE item_biblioteca
                SET pagina_atual = 100,
                    pagina_maxima = 100,
                    status_leitura = 'lendo'
                WHERE id = '40000000-0000-0000-0000-000000000001';

                DELETE FROM registro_leitura
                WHERE item_biblioteca_id IN (
                    SELECT id
                    FROM item_biblioteca
                    WHERE usuario_id = '00000000-0000-0000-0000-000000000001'
                      AND livro_id = '30000000-0000-0000-0000-000000000013'
                );

                DELETE FROM item_biblioteca
                WHERE usuario_id = '00000000-0000-0000-0000-000000000001'
                  AND livro_id = '30000000-0000-0000-0000-000000000013';
                """)
class ProgressoLeituraControllerIT {

    private static final String LIVRO_DOM_CASMURRO = "30000000-0000-0000-0000-000000000001";
    private static final String LIVRO_FORA_DA_BIBLIOTECA = "30000000-0000-0000-0000-000000000013";

    @Autowired private MockMvcTester mvc;

    @Test
    void deve_avancar_pagina_e_devolver_xp_das_paginas_novas() {
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":145}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.xpPaginas")
                .isEqualTo(45);

        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":145}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.xpPaginas")
                .isEqualTo(0);
    }

    @Test
    void deve_manter_pagina_maxima_quando_usuario_volta_no_livro() {
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":145}"))
                .hasStatusOk();

        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":100}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.xpPaginas")
                .isEqualTo(0);

        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":100}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.paginaMaximaAlcancada")
                .isEqualTo(145);
    }

    @Test
    void deve_pagar_bonus_de_conclusao_uma_unica_vez() {
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":256}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.xpTotal")
                .isEqualTo(306);

        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":256}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.xpConclusao")
                .isEqualTo(0);
    }

    @Test
    void deve_recusar_pagina_maior_que_total_com_422() {
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":300}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("PAGINA_INVALIDA");
    }

    @Test
    void deve_recusar_pagina_zero_com_422() {
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":0}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("PAGINA_INVALIDA");
    }

    @Test
    void deve_recusar_valor_nao_inteiro_com_422() {
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":\"abc\"}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("PAGINA_INVALIDA");
    }

    @Test
    void deve_devolver_404_para_livro_inexistente() {
        assertThat(
                        mvc.post()
                                .uri(
                                        "/api/livros/{livroId}/progresso",
                                        "30000000-0000-0000-0000-000000009999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":10}"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("LIVRO_NAO_ENCONTRADO");
    }

    @Test
    void deve_criar_item_biblioteca_quando_livro_ainda_nao_estiver_na_biblioteca() {
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_FORA_DA_BIBLIOTECA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":30}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.paginaMaximaAlcancada")
                .isEqualTo(30);
    }
}
