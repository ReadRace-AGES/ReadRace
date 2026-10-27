package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GET /api/busca")
class BuscaControllerIT {

    @Autowired private MockMvcTester mvc;

    @Autowired private JdbcTemplate jdbc;

    @Test
    void deve_encontrar_livro_pelo_titulo() {
        assertThat(mvc.get().uri("/api/busca?q=casMUR&tipo=livros"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens[0].titulo")
                .isEqualTo("Dom Casmurro");
    }

    @Test
    @Transactional
    void deve_encontrar_livro_somente_pelo_titulo_mesmo_sem_autor() {
        UUID livroId = inserirLivro("Título exclusivo xyz");

        assertThat(
                        mvc.get()
                                .uri("/api/busca")
                                .param("q", "TÍTULO EXCLUSIVO XYZ")
                                .param("tipo", "livros"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens[*].id")
                .asList()
                .containsExactly(livroId.toString());
    }

    @Test
    @Transactional
    void deve_encontrar_por_autor_sem_filtrar_os_demais_autores_da_resposta() {
        UUID livroId = inserirLivro("Título de teste", "Primeira pessoa", "Autora exclusiva xyz");

        var resposta =
                mvc.get()
                        .uri("/api/busca")
                        .param("q", "AUTORA EXCLUSIVA XYZ")
                        .param("tipo", "livros")
                        .exchange();
        assertThat(resposta)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens[*].id")
                .asList()
                .containsExactly(livroId.toString());
        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.itens[0].autor")
                .isEqualTo("Primeira pessoa, Autora exclusiva xyz");
    }

    @Test
    @Transactional
    void nao_deve_duplicar_livro_quando_titulo_e_varios_autores_correspondem() {
        UUID livroId =
                inserirLivro(
                        "Correspondência xyz", "Correspondência xyz A", "Correspondência xyz B");

        assertThat(
                        mvc.get()
                                .uri("/api/busca")
                                .param("q", "correspondência XYZ")
                                .param("tipo", "livros"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens[*].id")
                .asList()
                .containsExactly(livroId.toString());
    }

    @Test
    @Transactional
    void deve_preservar_a_distincao_de_acentos_no_titulo_e_no_autor() {
        inserirLivro("Título exclusivo xyz", "Autór exclusivo xyz");

        for (String termo : List.of("Titulo exclusivo xyz", "Autor exclusivo xyz")) {
            assertThat(mvc.get().uri("/api/busca").param("q", termo).param("tipo", "livros"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.itens")
                    .isEqualTo(List.of());
        }
    }

    private UUID inserirLivro(String titulo, String... autores) {
        UUID livroId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO livro (id, isbn, titulo, total_paginas) VALUES (?, '9999999999999', ?, 100)",
                livroId,
                titulo);
        for (int i = 0; i < autores.length; i++) {
            UUID autorId = UUID.randomUUID();
            jdbc.update("INSERT INTO autor (id, nome) VALUES (?, ?)", autorId, autores[i]);
            jdbc.update(
                    "INSERT INTO livro_autor (livro_id, autor_id, ordem) VALUES (?, ?, ?)",
                    livroId,
                    autorId,
                    i + 1);
        }
        return livroId;
    }

    @Test
    void deve_retornar_o_autor_do_livro() {
        assertThat(mvc.get().uri("/api/busca?q=casMUR&tipo=livros"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens[0].autor")
                .isEqualTo("Machado de Assis");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ana Silva", "nA sI"})
    void deve_encontrar_usuario_pelo_nome(String termo) {
        assertThat(mvc.get().uri("/api/busca").param("q", termo).param("tipo", "usuarios"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens[*].id")
                .asList()
                .containsExactlyInAnyOrder("00000000-0000-0000-0000-000000000002");
    }

    @ParameterizedTest
    @ValueSource(strings = {"anasilva", "nAsI"})
    void deve_encontrar_usuario_pelo_username(String termo) {
        assertThat(mvc.get().uri("/api/busca").param("q", termo).param("tipo", "usuarios"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens[*].id")
                .asList()
                .containsExactlyInAnyOrder("00000000-0000-0000-0000-000000000002");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Daniel Ribeiro", "danielribeiro", "eL rI", "eLrI"})
    void nao_deve_retornar_o_proprio_usuario_quando_so_ele_corresponde(String termo) {
        assertThat(mvc.get().uri("/api/busca").param("q", termo).param("tipo", "usuarios"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens")
                .isEqualTo(List.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Daniel Ribeiro", "eL rI"})
    @Sql(
            statements =
                    """
                    UPDATE usuario
                    SET nome = 'Daniel Ribeiro'
                    WHERE id = '00000000-0000-0000-0000-000000000002';
                    """)
    @Sql(
            statements =
                    """
                    UPDATE usuario
                    SET nome = 'Ana Silva'
                    WHERE id = '00000000-0000-0000-0000-000000000002';
                    """,
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void deve_retornar_outro_usuario_com_mesmo_nome_sem_retornar_o_proprio(String termo) {
        assertThat(mvc.get().uri("/api/busca").param("q", termo).param("tipo", "usuarios"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens[*].id")
                .asList()
                .containsExactlyInAnyOrder("00000000-0000-0000-0000-000000000002")
                .doesNotContain("00000000-0000-0000-0000-000000000001");
    }

    @Test
    void deve_encontrar_comunidade_pelo_nome() {
        assertThat(mvc.get().uri("/api/busca?q=brASile&tipo=comunidades"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens[0].nome")
                .isEqualTo("Literatura Brasileira");
    }

    @Test
    void deve_retornar_lista_vazia_quando_nao_encontrar_livros() {
        assertThat(mvc.get().uri("/api/busca?q=zzsemcorrespondencia43zz&tipo=livros"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens")
                .isEqualTo(List.of());
    }

    @Test
    void deve_retornar_lista_vazia_quando_nao_encontrar_usuarios() {
        assertThat(mvc.get().uri("/api/busca?q=zzsemcorrespondencia43zz&tipo=usuarios"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens")
                .isEqualTo(List.of());
    }

    @Test
    void deve_retornar_lista_vazia_quando_nao_encontrar_comunidades() {
        assertThat(mvc.get().uri("/api/busca?q=zzsemcorrespondencia43zz&tipo=comunidades"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens")
                .isEqualTo(List.of());
    }

    @Test
    void deve_retornar_400_quando_q_estiver_ausente() {
        assertThat(mvc.get().uri("/api/busca?tipo=livros"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("PARAMETRO_INVALIDO");
    }

    @Test
    void deve_retornar_400_quando_q_estiver_vazio() {
        assertThat(mvc.get().uri("/api/busca?q=&tipo=livros"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("PARAMETRO_INVALIDO");
    }

    @Test
    void deve_retornar_400_quando_tipo_estiver_ausente() {
        assertThat(mvc.get().uri("/api/busca?q=harry"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("PARAMETRO_INVALIDO");
    }

    @Test
    void deve_retornar_400_quando_tipo_estiver_vazio() {
        assertThat(mvc.get().uri("/api/busca?q=harry&tipo="))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("PARAMETRO_INVALIDO");
    }

    @Test
    void deve_retornar_400_quando_tipo_for_clubes() {
        assertThat(mvc.get().uri("/api/busca?q=harry&tipo=clubes"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("PARAMETRO_INVALIDO");
    }

    @Test
    void deve_retornar_400_quando_tipo_estiver_com_maiuscula() {
        assertThat(mvc.get().uri("/api/busca?q=harry&tipo=Livros"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("PARAMETRO_INVALIDO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ana Silva", "anasilva", "nA sI", "nAsI"})
    @Sql(
            statements =
                    """
                    UPDATE usuario
                    SET excluido_em = NOW()
                    WHERE id = '00000000-0000-0000-0000-000000000002';
                    """)
    @Sql(
            statements =
                    """
                    UPDATE usuario
                    SET excluido_em = NULL
                    WHERE id = '00000000-0000-0000-0000-000000000002';
                    """,
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void nao_deve_retornar_usuario_excluido(String termo) {
        assertThat(mvc.get().uri("/api/busca").param("q", termo).param("tipo", "usuarios"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens")
                .isEqualTo(List.of());
    }

    @Test
    @Sql(
            statements =
                    """
                    UPDATE comunidade
                    SET excluido_em = NOW()
                    WHERE id = '60000000-0000-0000-0000-000000000001';
                    """)
    @Sql(
            statements =
                    """
                    UPDATE comunidade
                    SET excluido_em = NULL
                    WHERE id = '60000000-0000-0000-0000-000000000001';
                    """,
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void nao_deve_retornar_comunidade_excluida() {
        assertThat(mvc.get().uri("/api/busca?q=brASile&tipo=comunidades"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens")
                .isEqualTo(List.of());
    }
}
