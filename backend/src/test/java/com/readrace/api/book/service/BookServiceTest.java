package com.readrace.api.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.readrace.api.book.dto.GoogleBookVolume;
import com.readrace.api.book.dto.GoogleBooksResponse;
import com.readrace.api.book.dto.VolumeInfo;
import com.readrace.api.book.port.BookSearchPort;
import com.readrace.api.exception.RecursoNaoEncontradoException;

/**
 * Teste unitário do service de livros. Mocka o {@link BookSearchPort} e verifica que os filtros
 * (title, author, genre, isbn) são convertidos para os qualificadores da Google Books API, que os
 * parâmetros são validados e que a ausência de filtro vira 400.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookService")
class BookServiceTest {

    @Mock private BookSearchPort bookSearchPort;

    @InjectMocks private BookService bookService;

    @Captor private ArgumentCaptor<String> queryCaptor;

    @Test
    void deve_converter_filtro_de_titulo_para_intitle() {
        when(bookSearchPort.search(queryCaptor.capture(), anyInt(), anyInt()))
                .thenReturn(GoogleBooksResponse.empty());

        bookService.search("senhor dos aneis", null, null, null, null, null, null);

        assertThat(queryCaptor.getValue()).isEqualTo("intitle:\"senhor dos aneis\"");
    }

    @Test
    void deve_converter_filtro_de_autor_para_inauthor() {
        when(bookSearchPort.search(queryCaptor.capture(), anyInt(), anyInt()))
                .thenReturn(GoogleBooksResponse.empty());

        bookService.search(null, "tolkien", null, null, null, null, null);

        assertThat(queryCaptor.getValue()).isEqualTo("inauthor:\"tolkien\"");
    }

    @Test
    void deve_converter_filtro_de_genero_para_subject() {
        when(bookSearchPort.search(queryCaptor.capture(), anyInt(), anyInt()))
                .thenReturn(GoogleBooksResponse.empty());

        bookService.search(null, null, "fantasia", null, null, null, null);

        assertThat(queryCaptor.getValue()).isEqualTo("subject:\"fantasia\"");
    }

    @Test
    void deve_converter_filtro_de_isbn_removendo_hifens_e_espacos() {
        when(bookSearchPort.search(queryCaptor.capture(), anyInt(), anyInt()))
                .thenReturn(GoogleBooksResponse.empty());

        bookService.search(null, null, null, "978-6586-064537", null, null, null);

        assertThat(queryCaptor.getValue()).isEqualTo("isbn:9786586064537");
    }

    @Test
    void deve_combinar_multiplos_filtros_com_espaco() {
        when(bookSearchPort.search(queryCaptor.capture(), anyInt(), anyInt()))
                .thenReturn(GoogleBooksResponse.empty());

        bookService.search(null, "tolkien", "fantasia", null, null, null, null);

        assertThat(queryCaptor.getValue()).isEqualTo("inauthor:\"tolkien\" subject:\"fantasia\"");
    }

    @Test
    void deve_lancar_400_quando_nenhum_filtro_for_informado() {
        assertThatThrownBy(() -> bookService.search(null, null, null, null, null, null, null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void deve_limitar_maxResults_ao_maximo_de_40() {
        when(bookSearchPort.search(eq("inauthor:\"tolkien\""), eq(40), anyInt()))
                .thenReturn(GoogleBooksResponse.empty());

        bookService.search(null, "tolkien", null, null, null, 999, null);

        // Se o clamp não funcionasse, o stub com eq(40) não casaria e o teste falharia.
        assertThat(true).isTrue();
    }

    @Test
    void deve_lancar_404_quando_volume_nao_existir() {
        when(bookSearchPort.getById("inexistente")).thenReturn(null);

        assertThatThrownBy(() -> bookService.getById("inexistente"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deve_devolver_volume_ao_buscar_por_id_existente() {
        GoogleBookVolume volume =
                GoogleBookVolume.of(
                        "abc123",
                        new VolumeInfo(
                                "1984", null, null, null, null, null, null, null, null, null, null,
                                null, null, null, null, null, null));
        when(bookSearchPort.getById("abc123")).thenReturn(volume);

        GoogleBookVolume resultado = bookService.getById("abc123");

        assertThat(resultado.id()).isEqualTo("abc123");
        assertThat(resultado.volumeInfo().title()).isEqualTo("1984");
    }
}
