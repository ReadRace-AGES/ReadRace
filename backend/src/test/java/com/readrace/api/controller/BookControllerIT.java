package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.readrace.api.TestcontainersConfiguration;

/**
 * Teste de integração do endpoint de busca de livros.
 *
 * <p>Roda SEM profile ativo, então o {@code LocalBooksAdapter} (@Profile("!prod")) responde a
 * partir do {@code books-seed.json} real — o mesmo comportamento que o CI exercita. Sobe o contexto
 * Spring completo e um PostgreSQL via Testcontainers, seguindo o padrão de {@code
 * ExemploControllerIT}.
 *
 * <p>Os erros são validados no formato padrão {@code {code, message}}.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GET /api/books/volumes")
class BookControllerIT {

    @Autowired private MockMvcTester mvc;

    @Test
    void deve_buscar_por_titulo_e_devolver_itens() {
        assertThat(mvc.get().uri("/api/books/volumes").param("title", "Dom Casmurro"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.items[0].volumeInfo.title")
                .asString()
                .containsIgnoringCase("dom casmurro");
    }

    @Test
    void deve_buscar_por_autor_e_devolver_itens() {
        assertThat(mvc.get().uri("/api/books/volumes").param("author", "George Orwell"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalItems")
                .asNumber()
                .satisfies(total -> assertThat(total.intValue()).isPositive());
    }

    @Test
    void deve_buscar_por_genero_e_devolver_itens() {
        assertThat(mvc.get().uri("/api/books/volumes").param("genre", "Fantasia"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalItems")
                .asNumber()
                .satisfies(total -> assertThat(total.intValue()).isPositive());
    }

    @Test
    void deve_buscar_por_isbn_e_devolver_o_livro_certo() {
        assertThat(mvc.get().uri("/api/books/volumes").param("isbn", "9786586064537"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.items[0].volumeInfo.title")
                .asString()
                .containsIgnoringCase("1984");
    }

    @Test
    void deve_buscar_por_texto_livre() {
        assertThat(mvc.get().uri("/api/books/volumes").param("q", "Machado"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.totalItems")
                .asNumber()
                .satisfies(total -> assertThat(total.intValue()).isPositive());
    }

    @Test
    void deve_devolver_400_quando_nenhum_filtro_for_informado() {
        assertThat(mvc.get().uri("/api/books/volumes"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    void deve_buscar_volume_por_id_do_seed() {
        assertThat(mvc.get().uri("/api/books/volumes/{id}", "sci001"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo("sci001");
    }

    @Test
    void deve_devolver_404_com_code_resource_not_found_para_id_inexistente() {
        assertThat(mvc.get().uri("/api/books/volumes/{id}", "id-que-nao-existe"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }
}
