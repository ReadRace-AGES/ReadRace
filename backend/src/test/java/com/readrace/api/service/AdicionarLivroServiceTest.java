package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import com.readrace.api.model.Autor;
import com.readrace.api.model.ItemBiblioteca;
import com.readrace.api.model.Livro;
import com.readrace.api.model.LivroAutor;
import com.readrace.api.model.StatusLeitura;
import com.readrace.api.model.UsuarioId;
import com.readrace.api.repository.AutorRepository;
import com.readrace.api.repository.GeneroRepository;
import com.readrace.api.repository.ItemBibliotecaRepository;
import com.readrace.api.repository.LivroAutorRepository;
import com.readrace.api.repository.LivroRepository;

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

    private VolumeInfo volumeInfo(
            String titulo,
            List<String> autores,
            List<IndustryIdentifier> identificadores,
            Integer paginas) {
        return new VolumeInfo(
                titulo,
                null,
                autores,
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

    private VolumeInfo volumeInfo(List<IndustryIdentifier> identificadores, Integer paginas) {
        return volumeInfo("Título de teste", List.of("Autor de teste"), identificadores, paginas);
    }

    private void configurarCatalogo(
            String volumeId, String titulo, List<String> autores, String isbn) {
        VolumeInfo info =
                volumeInfo(titulo, autores, List.of(new IndustryIdentifier("ISBN_13", isbn)), 200);

        when(bookSearchPort.getById(volumeId))
                .thenReturn(Optional.of(GoogleBookVolume.of(volumeId, info)));
    }

    private Livro livroExistente(UUID livroId, String titulo, String nomePrimeiroAutor) {
        Livro livro = mock(Livro.class);
        Autor autor = mock(Autor.class);
        LivroAutor vinculo = mock(LivroAutor.class);

        when(livro.getTitulo()).thenReturn(titulo);
        when(livro.getLivroAutores()).thenReturn(List.of(vinculo));
        when(vinculo.getAutor()).thenReturn(autor);
        when(autor.getNome()).thenReturn(nomePrimeiroAutor);

        return livro;
    }

    private void configurarBiblioteca(UUID livroId) {
        UUID usuarioId = UUID.randomUUID();

        when(usuarioAtual.idDoUsuarioAtual()).thenReturn(new UsuarioId(usuarioId));

        ItemBiblioteca item = mock(ItemBiblioteca.class);

        when(item.getStatusLeitura()).thenReturn(StatusLeitura.desejo);

        when(itemBibliotecaRepository.findByUsuarioIdAndLivroId(usuarioId, livroId))
                .thenReturn(Optional.of(item));
    }

    @Test
    @DisplayName("lista inválida não consulta o catálogo")
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
    @DisplayName("volume inexistente devolve LivroNaoEncontradoException")
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
    @DisplayName("volume sem ISBN é recusado")
    void deve_recusar_volume_sem_isbn() {
        when(bookSearchPort.getById("v1"))
                .thenReturn(Optional.of(GoogleBookVolume.of("v1", volumeInfo(List.of(), 200))));

        assertThatThrownBy(() -> service().adicionar(new AdicionarLivroRequest("v1", "desejo")))
                .isInstanceOf(LivroSemIsbnOuPaginasException.class);
    }

    @Test
    @DisplayName("volume sem páginas é recusado")
    void deve_recusar_volume_sem_paginas() {
        when(bookSearchPort.getById("v1"))
                .thenReturn(
                        Optional.of(
                                GoogleBookVolume.of(
                                        "v1",
                                        volumeInfo(
                                                List.of(
                                                        new IndustryIdentifier(
                                                                "ISBN_13", "9786586490077")),
                                                null))));

        assertThatThrownBy(() -> service().adicionar(new AdicionarLivroRequest("v1", "desejo")))
                .isInstanceOf(LivroSemIsbnOuPaginasException.class);
    }

    @Test
    @DisplayName("volume com zero páginas é recusado")
    void deve_recusar_paginas_zero() {
        when(bookSearchPort.getById("v1"))
                .thenReturn(
                        Optional.of(
                                GoogleBookVolume.of(
                                        "v1",
                                        volumeInfo(
                                                List.of(
                                                        new IndustryIdentifier(
                                                                "ISBN_13", "9786586490077")),
                                                0))));

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
                                                    List.of("Autor de teste"),
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
                                                List.of("Autor de teste"),
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

        Livro livro = mock(Livro.class);
        UUID livroId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        when(livro.getId()).thenReturn(livroId);
        when(livroRepository.findByIsbn(esperado)).thenReturn(Optional.of(livro));

        when(usuarioAtual.idDoUsuarioAtual()).thenReturn(new UsuarioId(usuarioId));

        ItemBiblioteca item = mock(ItemBiblioteca.class);

        when(item.getStatusLeitura()).thenReturn(StatusLeitura.desejo);

        when(itemBibliotecaRepository.findByUsuarioIdAndLivroId(usuarioId, livroId))
                .thenReturn(Optional.of(item));

        service().adicionar(new AdicionarLivroRequest("v1", "desejo"));

        verify(livroRepository).findByIsbn(esperado);
    }

    @Test
    @DisplayName("ISBN igual reutiliza o livro sem consultar título e autor")
    void deve_priorizar_isbn_existente() {
        String isbn = "9786586490077";
        UUID livroId = UUID.randomUUID();

        configurarCatalogo("br001", "Dom Casmurro", List.of("Machado de Assis"), isbn);

        Livro livro = mock(Livro.class);
        when(livro.getId()).thenReturn(livroId);

        when(livroRepository.findByIsbn(isbn)).thenReturn(Optional.of(livro));

        configurarBiblioteca(livroId);

        var resposta = service().adicionar(new AdicionarLivroRequest("br001", "desejo"));

        assertThat(resposta.livroId()).isEqualTo(livroId);

        verify(livroRepository, never()).buscarLivrosComAutores();
        verify(livroRepository, never())
                .criarSeAusente(
                        any(UUID.class), anyString(), anyString(), any(Integer.class), any());
    }

    @Test
    @DisplayName("ISBN diferente e mesmo título e autor reutiliza livro")
    void deve_reutilizar_livro_por_titulo_e_autor() {
        String isbnCatalogo = "9786586490077";
        UUID livroId = UUID.randomUUID();

        configurarCatalogo("br001", "Dom Casmurro", List.of("Machado de Assis"), isbnCatalogo);

        Livro existente = livroExistente(livroId, "Dom Casmurro", "Machado de Assis");

        when(existente.getId()).thenReturn(livroId);

        when(livroRepository.findByIsbn(isbnCatalogo)).thenReturn(Optional.empty());

        when(livroRepository.buscarLivrosComAutores()).thenReturn(List.of(existente));

        configurarBiblioteca(livroId);

        var resposta = service().adicionar(new AdicionarLivroRequest("br001", "desejo"));

        assertThat(resposta.livroId()).isEqualTo(livroId);

        verify(livroRepository, never())
                .criarSeAusente(
                        any(UUID.class), anyString(), anyString(), any(Integer.class), any());
    }

    @Test
    @DisplayName("ignora maiúsculas, acentos, espaços e pontuação")
    void deve_normalizar_titulo_e_primeiro_autor() {
        String isbnCatalogo = "9788525053251";
        UUID livroId = UUID.randomUUID();

        configurarCatalogo(
                "br002",
                "MEMÓRIAS PÓSTUMAS DE BRÁS CUBAS",
                List.of("MACHADO DE ASSIS"),
                isbnCatalogo);

        Livro existente =
                livroExistente(livroId, "Memorias - Postumas de Bras Cubas!", "Machado, de Assis");

        when(existente.getId()).thenReturn(livroId);

        when(livroRepository.findByIsbn(isbnCatalogo)).thenReturn(Optional.empty());

        when(livroRepository.buscarLivrosComAutores()).thenReturn(List.of(existente));

        configurarBiblioteca(livroId);

        var resposta = service().adicionar(new AdicionarLivroRequest("br002", "desejo"));

        assertThat(resposta.livroId()).isEqualTo(livroId);

        verify(livroRepository, never())
                .criarSeAusente(
                        any(UUID.class), anyString(), anyString(), any(Integer.class), any());
    }

    @Test
    @DisplayName("mesmo título com primeiro autor diferente não reutiliza")
    void deve_diferenciar_livros_com_autores_diferentes() {
        String isbnCatalogo = "9786586490077";

        configurarCatalogo("br001", "Dom Casmurro", List.of("Machado de Assis"), isbnCatalogo);

        Livro outroAutor = livroExistente(UUID.randomUUID(), "Dom Casmurro", "Outro Autor");

        when(livroRepository.findByIsbn(isbnCatalogo)).thenReturn(Optional.empty());

        when(livroRepository.buscarLivrosComAutores()).thenReturn(List.of(outroAutor));

        assertThatThrownBy(() -> service().adicionar(new AdicionarLivroRequest("br001", "desejo")))
                .isInstanceOf(java.util.NoSuchElementException.class);

        verify(livroRepository)
                .criarSeAusente(
                        any(UUID.class),
                        org.mockito.ArgumentMatchers.eq(isbnCatalogo),
                        org.mockito.ArgumentMatchers.eq("Dom Casmurro"),
                        org.mockito.ArgumentMatchers.eq(200),
                        any());
    }
}
