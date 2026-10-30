package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
        return volumeInfo("Título de teste", identificadores, paginas);
    }

    private VolumeInfo volumeInfo(
            String titulo, List<IndustryIdentifier> identificadores, Integer paginas) {
        return new VolumeInfo(
                titulo,
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

    @Test
    void deve_recusar_isbn_nulo_vazio_ou_em_branco_sem_gravar() {
        for (String isbn : new String[] {null, "", "   "}) {
            when(bookSearchPort.getById("v1"))
                    .thenReturn(
                            Optional.of(
                                    GoogleBookVolume.of(
                                            "v1",
                                            volumeInfo(
                                                    List.of(
                                                            new IndustryIdentifier(
                                                                    "ISBN_13", isbn)),
                                                    200))));
            assertThatThrownBy(() -> service().adicionar(new AdicionarLivroRequest("v1", "desejo")))
                    .isInstanceOf(LivroSemIsbnOuPaginasException.class);
        }
        verifyNoInteractions(livroRepository, itemBibliotecaRepository);
    }

    @Test
    void deve_recusar_paginas_negativas_sem_gravar() {
        when(bookSearchPort.getById("v1"))
                .thenReturn(
                        Optional.of(
                                GoogleBookVolume.of(
                                        "v1",
                                        volumeInfo(
                                                List.of(
                                                        new IndustryIdentifier(
                                                                "ISBN_13", "9786586490077")),
                                                -1))));
        assertThatThrownBy(() -> service().adicionar(new AdicionarLivroRequest("v1", "desejo")))
                .isInstanceOf(LivroSemIsbnOuPaginasException.class);
        verifyNoInteractions(livroRepository, itemBibliotecaRepository);
    }

    @Test
    @DisplayName("volume sem título (nulo, vazio ou em branco) responde como livro sem ISBN")
    void deve_recusar_volume_sem_titulo_sem_gravar() {
        for (String titulo : new String[] {null, "", "   "}) {
            when(bookSearchPort.getById("v1"))
                    .thenReturn(
                            Optional.of(
                                    GoogleBookVolume.of(
                                            "v1",
                                            volumeInfo(
                                                    titulo,
                                                    List.of(
                                                            new IndustryIdentifier(
                                                                    "ISBN_13", "9786586490077")),
                                                    200))));
            assertThatThrownBy(() -> service().adicionar(new AdicionarLivroRequest("v1", "desejo")))
                    .isInstanceOf(LivroSemIsbnOuPaginasException.class);
        }
        verifyNoInteractions(livroRepository, itemBibliotecaRepository);
    }

    @Test
    @DisplayName("título com mais de 255 caracteres é cortado em 255 ao criar o livro")
    void deve_cortar_titulo_longo() {
        assertThat(tituloGravadoPara("t".repeat(300))).hasSize(255);
    }

    @Test
    @DisplayName("o corte do título conta caracteres, sem partir um par substituto (emoji)")
    void deve_cortar_titulo_sem_partir_emoji() {
        String titulo = "a".repeat(254) + "😀😀";

        String gravado = tituloGravadoPara(titulo);

        assertThat(gravado.codePointCount(0, gravado.length())).isEqualTo(255);
        assertThat(gravado).endsWith("😀").doesNotEndWith("😀😀");
    }

    @Test
    @DisplayName("título com espaços nas pontas é gravado sem eles")
    void deve_aparar_o_titulo() {
        assertThat(tituloGravadoPara("  Dom Casmurro  ")).isEqualTo("Dom Casmurro");
    }

    private String tituloGravadoPara(String titulo) {
        String isbn = "9786586490077";
        when(bookSearchPort.getById("v1"))
                .thenReturn(
                        Optional.of(
                                GoogleBookVolume.of(
                                        "v1",
                                        volumeInfo(
                                                titulo,
                                                List.of(new IndustryIdentifier("ISBN_13", isbn)),
                                                200))));
        var livro = mock(com.readrace.api.model.Livro.class);
        var livroId = java.util.UUID.randomUUID();
        var usuarioId = java.util.UUID.randomUUID();
        when(livro.getId()).thenReturn(livroId);
        when(livroRepository.findByIsbn(isbn)).thenReturn(Optional.empty(), Optional.of(livro));
        when(usuarioAtual.idDoUsuarioAtual())
                .thenReturn(new com.readrace.api.model.UsuarioId(usuarioId));
        var item = mock(com.readrace.api.model.ItemBiblioteca.class);
        when(item.getStatusLeitura()).thenReturn(com.readrace.api.model.StatusLeitura.desejo);
        when(itemBibliotecaRepository.findByUsuarioIdAndLivroId(usuarioId, livroId))
                .thenReturn(Optional.of(item));

        service().adicionar(new AdicionarLivroRequest("v1", "desejo"));

        var capturado = ArgumentCaptor.forClass(String.class);
        verify(livroRepository)
                .criarSeAusente(any(), eq(isbn), capturado.capture(), eq(200), any());
        return capturado.getValue();
    }

    @Test
    void deve_normalizar_isbn13_e_preferir_ao_isbn10() {
        conferirIsbn(
                List.of(
                        new IndustryIdentifier("ISBN_10", "8535910663"),
                        new IndustryIdentifier("ISBN_13", " 9786586490077 ")),
                "9786586490077");
    }

    @Test
    void deve_usar_isbn10_quando_isbn13_estiver_em_branco() {
        conferirIsbn(
                List.of(
                        new IndustryIdentifier("ISBN_13", "   "),
                        new IndustryIdentifier("ISBN_10", " 8535910663 ")),
                "8535910663");
    }

    private void conferirIsbn(List<IndustryIdentifier> identificadores, String esperado) {
        when(bookSearchPort.getById("v1"))
                .thenReturn(
                        Optional.of(GoogleBookVolume.of("v1", volumeInfo(identificadores, 200))));
        var livro = mock(com.readrace.api.model.Livro.class);
        var livroId = java.util.UUID.randomUUID();
        var usuarioId = java.util.UUID.randomUUID();
        when(livro.getId()).thenReturn(livroId);
        when(livroRepository.findByIsbn(esperado)).thenReturn(Optional.of(livro));
        when(usuarioAtual.idDoUsuarioAtual())
                .thenReturn(new com.readrace.api.model.UsuarioId(usuarioId));
        var item = mock(com.readrace.api.model.ItemBiblioteca.class);
        when(item.getStatusLeitura()).thenReturn(com.readrace.api.model.StatusLeitura.desejo);
        when(itemBibliotecaRepository.findByUsuarioIdAndLivroId(usuarioId, livroId))
                .thenReturn(Optional.of(item));
        service().adicionar(new AdicionarLivroRequest("v1", "desejo"));
        verify(livroRepository).findByIsbn(esperado);
    }
}
