package com.readrace.api.book.adapter.local;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Teste unitário do parser de query. Sem Spring, sem banco. Prova que os qualificadores da Google
 * Books ({@code intitle}, {@code inauthor}, {@code subject}, {@code isbn}) são extraídos
 * corretamente e que o texto livre continua separado.
 */
@DisplayName("SearchQuery")
class SearchQueryTest {

    @Test
    void deve_tratar_query_sem_qualificador_como_busca_geral() {
        SearchQuery query = SearchQuery.parse("tolkien");

        assertThat(query.isGeneralSearch()).isTrue();
        assertThat(query.freeText()).isEqualTo("tolkien");
        assertThat(query.titleTerms()).isEmpty();
        assertThat(query.authorTerms()).isEmpty();
        assertThat(query.subjectTerms()).isEmpty();
        assertThat(query.isbnTerms()).isEmpty();
    }

    @Test
    void deve_extrair_qualificador_de_titulo() {
        SearchQuery query = SearchQuery.parse("intitle:\"senhor dos aneis\"");

        assertThat(query.isGeneralSearch()).isFalse();
        assertThat(query.titleTerms()).containsExactly("senhor dos aneis");
        assertThat(query.freeText()).isEmpty();
    }

    @Test
    void deve_extrair_qualificador_de_autor() {
        SearchQuery query = SearchQuery.parse("inauthor:tolkien");

        assertThat(query.authorTerms()).containsExactly("tolkien");
    }

    @Test
    void deve_extrair_qualificador_de_genero() {
        SearchQuery query = SearchQuery.parse("subject:fantasia");

        assertThat(query.subjectTerms()).containsExactly("fantasia");
    }

    @Test
    void deve_extrair_qualificador_de_isbn() {
        SearchQuery query = SearchQuery.parse("isbn:9786586064537");

        assertThat(query.isbnTerms()).containsExactly("9786586064537");
        assertThat(query.isGeneralSearch()).isFalse();
    }

    @Test
    void deve_extrair_multiplos_qualificadores_combinados() {
        SearchQuery query = SearchQuery.parse("intitle:\"harry\" inauthor:\"rowling\"");

        assertThat(query.titleTerms()).containsExactly("harry");
        assertThat(query.authorTerms()).containsExactly("rowling");
    }
}
