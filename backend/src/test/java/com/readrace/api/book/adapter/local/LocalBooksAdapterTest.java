package com.readrace.api.book.adapter.local;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readrace.api.book.dto.GoogleBookVolume;
import com.readrace.api.book.dto.GoogleBooksResponse;

/**
 * Teste unitário do mock local. Carrega o books-seed.json real e prova que a busca por título,
 * autor, gênero e ISBN funciona, além da paginação e da busca por ID. Sem Spring, sem banco.
 */
@DisplayName("LocalBooksAdapter")
class LocalBooksAdapterTest {

    private LocalBooksAdapter adapter;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper =
                new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        adapter = new LocalBooksAdapter(objectMapper);
        adapter.loadBooks();
    }

    @Test
    void deve_carregar_os_livros_do_seed() {
        GoogleBooksResponse resposta = adapter.search("intitle:\"1984\"", 10, 0);

        assertThat(resposta.totalItems()).isPositive();
    }

    @Test
    void deve_buscar_por_titulo() {
        GoogleBooksResponse resposta = adapter.search("intitle:\"Dom Casmurro\"", 10, 0);

        assertThat(resposta.items())
                .isNotEmpty()
                .allSatisfy(
                        volume ->
                                assertThat(volume.volumeInfo().title())
                                        .containsIgnoringCase("dom casmurro"));
    }

    @Test
    void deve_buscar_por_autor() {
        GoogleBooksResponse resposta = adapter.search("inauthor:\"Machado de Assis\"", 40, 0);

        assertThat(resposta.items())
                .isNotEmpty()
                .allSatisfy(
                        volume ->
                                assertThat(volume.volumeInfo().authors())
                                        .anyMatch(a -> a.contains("Machado")));
    }

    @Test
    void deve_buscar_por_genero() {
        GoogleBooksResponse resposta = adapter.search("subject:\"Fantasia\"", 40, 0);

        assertThat(resposta.items())
                .isNotEmpty()
                .allSatisfy(
                        volume ->
                                assertThat(volume.volumeInfo().categories())
                                        .anyMatch(c -> c.contains("Fantasia")));
    }

    @Test
    void deve_buscar_por_isbn() {
        // ISBN real do 1984 no seed
        GoogleBooksResponse resposta = adapter.search("isbn:9786586064537", 10, 0);

        assertThat(resposta.items())
                .singleElement()
                .satisfies(
                        volume ->
                                assertThat(volume.volumeInfo().title())
                                        .containsIgnoringCase("1984"));
    }

    @Test
    void deve_ignorar_acentos_e_case_na_busca_geral() {
        GoogleBooksResponse comAcento = adapter.search("história", 40, 0);
        GoogleBooksResponse semAcento = adapter.search("HISTORIA", 40, 0);

        assertThat(comAcento.totalItems()).isEqualTo(semAcento.totalItems());
    }

    @Test
    void deve_retornar_vazio_para_query_sem_correspondencia() {
        GoogleBooksResponse resposta = adapter.search("intitle:\"livro inexistente xyz\"", 10, 0);

        assertThat(resposta.totalItems()).isZero();
        assertThat(resposta.items()).isEmpty();
    }

    @Test
    void deve_retornar_vazio_para_query_em_branco() {
        assertThat(adapter.search("", 10, 0).totalItems()).isZero();
        assertThat(adapter.search(null, 10, 0).totalItems()).isZero();
    }

    @Test
    void deve_respeitar_o_maxResults_na_paginacao() {
        // "a" aparece em muitos livros — busca geral ampla
        GoogleBooksResponse resposta = adapter.search("a", 3, 0);

        assertThat(resposta.items()).hasSizeLessThanOrEqualTo(3);
        // totalItems reflete o total encontrado, não o tamanho da página
        assertThat(resposta.totalItems()).isGreaterThanOrEqualTo(resposta.items().size());
    }

    @Test
    void deve_avancar_a_pagina_com_startIndex() {
        GoogleBooksResponse pagina1 = adapter.search("a", 2, 0);
        GoogleBooksResponse pagina2 = adapter.search("a", 2, 2);

        assertThat(pagina1.items()).isNotEqualTo(pagina2.items());
    }

    @Test
    void deve_buscar_volume_por_id() {
        GoogleBookVolume volume = adapter.getById("sci001");

        assertThat(volume).isNotNull();
        assertThat(volume.id()).isEqualTo("sci001");
    }

    @Test
    void deve_devolver_null_para_id_inexistente() {
        assertThat(adapter.getById("nao-existe")).isNull();
        assertThat(adapter.getById(null)).isNull();
    }
}
