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
import com.readrace.api.model.Usuario;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("POST /api/livros/{livroId}/progresso")
class ProgressoLeituraControllerIT {

    // O seed inicia Dom Casmurro na página 145; cada teste desfaz suas alterações por rollback.
    private static final String LIVRO_DOM_CASMURRO = "30000000-0000-0000-0000-000000000001";
    private static final String LIVRO_FORA_DA_BIBLIOTECA = "30000000-0000-0000-0000-000000000013";
    private static final UUID USUARIO_FIXO =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

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
        // 111 de páginas novas (256 - 145) + 150 de bônus = 261, somado ao usuário na mesma
        // transação do registro: 2450 + 261 = 2711, ainda dentro do nível 7 (fecha em 2762).
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":256}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("xpTotal", 261)
                .containsEntry("xpDoUsuario", 2711)
                .containsEntry("nivel", 7)
                .containsEntry("subiuDeNivel", false);

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

        // A transação inteira volta atrás: nem o registro, nem o XP, nem o nível são gravados.
        entityManager.clear();
        Usuario usuario = entityManager.find(Usuario.class, USUARIO_FIXO);
        assertThat(usuario.getXpTotal()).isEqualTo(2450);
        assertThat(usuario.getNivel()).isEqualTo(7);
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
    void deve_somar_xp_ao_usuario_sem_subir_de_nivel() {
        // Usuário fixo do seed: 2450 de XP, nível 7 (recalculado pela V7). 145 -> 200 em
        // Dom Casmurro paga 55 de XP, os mesmos números do exemplo da issue #95.
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":200}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("xpPaginas", 55)
                .containsEntry("xpDoUsuario", 2505)
                .containsEntry("nivel", 7)
                .containsEntry("xpNoNivel", 576)
                .containsEntry("xpDoNivel", 833)
                .containsEntry("subiuDeNivel", false);
    }

    @Test
    void deve_subir_de_nivel_quando_xp_total_ultrapassa_o_limite() {
        String livroCrimeECastigo = "30000000-0000-0000-0000-000000000006";

        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":200}"))
                .hasStatusOk();
        entityManager.flush();
        entityManager.clear();

        // 2505 (depois do registro acima) + 310 (620 - pagina_maxima 310 do seed) = 2815,
        // que passa dos 2762 que fecham o nível 7.
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", livroCrimeECastigo)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":620}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("xpPaginas", 310)
                .containsEntry("xpDoUsuario", 2815)
                .containsEntry("nivel", 8)
                .containsEntry("xpNoNivel", 53)
                .containsEntry("xpDoNivel", 1018)
                .containsEntry("subiuDeNivel", true);
    }

    @Test
    void nao_deve_alterar_xp_do_usuario_quando_pagina_ja_foi_lida() {
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_DOM_CASMURRO)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":100}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("xpDoUsuario", 2450)
                .containsEntry("nivel", 7)
                .containsEntry("subiuDeNivel", false);
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
