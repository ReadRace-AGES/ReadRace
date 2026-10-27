package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

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

import com.readrace.api.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GET /api/busca")
class BuscaControllerIT {

    @Autowired private MockMvcTester mvc;

    @Test
    void deve_encontrar_livro_pelo_titulo() {
        assertThat(mvc.get().uri("/api/busca?q=casMUR&tipo=livros"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens[0].titulo")
                .isEqualTo("Dom Casmurro");
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
