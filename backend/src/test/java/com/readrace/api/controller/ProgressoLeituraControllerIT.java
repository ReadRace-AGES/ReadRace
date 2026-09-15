package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.model.ItemBiblioteca;
import com.readrace.api.model.StatusLeitura;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("POST /api/livros/{livroId}/progresso")
class ProgressoLeituraControllerIT {

    // O seed inicia Dom Casmurro na página 145; cada teste desfaz suas alterações por rollback.
    private static final String LIVRO_DOM_CASMURRO = "30000000-0000-0000-0000-000000000001";
    private static final String LIVRO_FORA_DA_BIBLIOTECA = "30000000-0000-0000-0000-000000000013";

    @Autowired private MockMvcTester mvc;
    @Autowired private EntityManager entityManager;

    @AfterEach
    void deve_persistir_alteracoes_antes_do_rollback() {
        entityManager.flush();
    }

    @Test
    void deve_avancar_pagina_e_devolver_xp_das_paginas_novas() {
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":190}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.xpPaginas")
                .isEqualTo(45);

        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":190}"))
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
                                .content("{\"pagina\":190}"))
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
                .isEqualTo(190);
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
                .isEqualTo(261);

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
    void nao_deve_repetir_bonus_apos_retroceder_e_concluir_novamente() {
        for (int pagina : new int[] {256, 100}) {
            assertThat(
                            mvc.post()
                                    .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"pagina\":" + pagina + "}"))
                    .hasStatusOk();
            entityManager.flush();
            entityManager.clear();
        }
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":256}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.xpTotal")
                .isEqualTo(0);
    }

    @Test
    void deve_manter_livro_concluido_apos_reler_pagina_anterior() {
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":256}"))
                .hasStatusOk();
        entityManager.flush();
        entityManager.clear();

        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":100}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("concluido", true)
                .containsEntry("paginaAtual", 100)
                .containsEntry("paginaMaximaAlcancada", 256)
                .containsEntry("xpTotal", 0);
        entityManager.flush();
        entityManager.clear();

        ItemBiblioteca item =
                entityManager.find(
                        ItemBiblioteca.class,
                        UUID.fromString("40000000-0000-0000-0000-000000000001"));
        assertThat(item.getStatusLeitura()).isEqualTo(StatusLeitura.lido);
        assertThat(item.getPaginaAtual()).isEqualTo(100);
        assertThat(item.getPaginaMaxima()).isEqualTo(256);
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
