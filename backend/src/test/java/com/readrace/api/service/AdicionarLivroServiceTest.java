package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.readrace.api.dto.GoogleBookVolume;
import com.readrace.api.dto.IndustryIdentifier;
import com.readrace.api.dto.VolumeInfo;
import com.readrace.api.dto.request.AdicionarLivroRequest;
import com.readrace.api.exception.ListaInvalidaException;
import com.readrace.api.exception.LivroNaoEncontradoException;
import com.readrace.api.exception.LivroSemIsbnOuPaginasException;
import com.readrace.api.repository.AutorRepository;
import com.readrace.api.repository.GeneroRepository;
import com.readrace.api.repository.ItemBibliotecaRepository;
import com.readrace.api.repository.LivroAutorRepository;
import com.readrace.api.repository.LivroRepository;

/**
 * Teste unitário das validações de {@link AdicionarLivroService}. Mocka o {@link BookSearchPort}
 * para cobrir volumes sem ISBN/páginas, algo que não ocorre no catálogo local de teste (#155).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdicionarLivroService")
class AdicionarLivroServiceTest {

    @Mock private BookSearchPort bookSearchPort;
    @Mock private LivroRepository livroRepository;
    @Mock private AutorRepository autorRepository;
    @Mock private LivroAutorRepository livroAutorRepository;
    @Mock private GeneroRepository generoRepository;
    @Mock private ItemBibliotecaRepository itemBibliotecaRepository;
    @Mock private UsuarioAtual usuarioAtual;

    private AdicionarLivroService service() {
        return new AdicionarLivroService(
                bookSearchPort,
                livroRepository,
                autorRepository,
                livroAutorRepository,
                generoRepository,
                itemBibliotecaRepository,
                usuarioAtual);
    }

    private VolumeInfo volumeInfo(List<IndustryIdentifier> identificadores, Integer paginas) {
        return new VolumeInfo(
                "Título de teste",
                null,
                List.of("Autor de teste"),
                null,
                null,
                null,
                identificadores,
                paginas,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    @DisplayName(
            "lista fora de lido/desejo/favorito devolve ListaInvalidaException, sem consultar o catálogo")
    void deve_recusar_lista_invalida() {
        assertThatThrownBy(() -> service().adicionar(new AdicionarLivroRequest("v1", "lendo")))
                .isInstanceOf(ListaInvalidaException.class);
    }

    @Test
    void deve_recusar_lista_nula() {
        assertThatThrownBy(() -> service().adicionar(new AdicionarLivroRequest("v1", null)))
                .isInstanceOf(ListaInvalidaException.class);
    }

    @Test
    @DisplayName("volumeId que o catálogo não conhece devolve LivroNaoEncontradoException")
    void deve_recusar_volume_inexistente() {
        when(bookSearchPort.getById("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service()
                                        .adicionar(
                                                new AdicionarLivroRequest("inexistente", "desejo")))
                .isInstanceOf(LivroNaoEncontradoException.class);
    }

    @Test
    @DisplayName("volume sem industryIdentifiers devolve LivroSemIsbnOuPaginasException")
    void deve_recusar_volume_sem_isbn() {
        GoogleBookVolume volume = GoogleBookVolume.of("v1", volumeInfo(List.of(), 200));
        when(bookSearchPort.getById("v1")).thenReturn(Optional.of(volume));

        assertThatThrownBy(() -> service().adicionar(new AdicionarLivroRequest("v1", "desejo")))
                .isInstanceOf(LivroSemIsbnOuPaginasException.class);
    }

    @Test
    @DisplayName("volume sem pageCount devolve LivroSemIsbnOuPaginasException mesmo com ISBN")
    void deve_recusar_volume_sem_paginas() {
        GoogleBookVolume volume =
                GoogleBookVolume.of(
                        "v1",
                        volumeInfo(
                                List.of(new IndustryIdentifier("ISBN_13", "9786586490077")), null));
        when(bookSearchPort.getById("v1")).thenReturn(Optional.of(volume));

        assertThatThrownBy(() -> service().adicionar(new AdicionarLivroRequest("v1", "desejo")))
                .isInstanceOf(LivroSemIsbnOuPaginasException.class);
    }

    @Test
    @DisplayName("pageCount zero também não cabe na biblioteca")
    void deve_recusar_paginas_zero() {
        GoogleBookVolume volume =
                GoogleBookVolume.of(
                        "v1",
                        volumeInfo(List.of(new IndustryIdentifier("ISBN_13", "9786586490077")), 0));
        when(bookSearchPort.getById("v1")).thenReturn(Optional.of(volume));

        assertThatThrownBy(() -> service().adicionar(new AdicionarLivroRequest("v1", "desejo")))
                .isInstanceOf(LivroSemIsbnOuPaginasException.class);
    }
}
