package com.readrace.api.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.GoogleBookVolume;
import com.readrace.api.dto.IndustryIdentifier;
import com.readrace.api.dto.VolumeInfo;
import com.readrace.api.dto.request.AdicionarLivroRequest;
import com.readrace.api.dto.response.AdicionarLivroResponse;
import com.readrace.api.exception.ListaInvalidaException;
import com.readrace.api.exception.LivroNaoEncontradoException;
import com.readrace.api.exception.LivroSemIsbnOuPaginasException;
import com.readrace.api.model.Autor;
import com.readrace.api.model.ItemBiblioteca;
import com.readrace.api.model.Livro;
import com.readrace.api.model.LivroAutor;
import com.readrace.api.model.StatusLeitura;
import com.readrace.api.repository.AutorRepository;
import com.readrace.api.repository.GeneroRepository;
import com.readrace.api.repository.ItemBibliotecaRepository;
import com.readrace.api.repository.LivroAutorRepository;
import com.readrace.api.repository.LivroRepository;

/**
 * Adiciona um livro do catálogo externo à biblioteca do usuário atual (#155).
 *
 * <p>Não mexe em XP, sequência, desafio ou conquista: marcar como lido aqui não dá XP, só o
 * registro de leitura dá.
 */
@Service
public class AdicionarLivroService {

    private static final Set<String> LISTAS_VALIDAS = Set.of("lido", "desejo", "favorito");
    private static final int TITULO_MAXIMO = 255;

    private final BookSearchPort bookSearchPort;
    private final LivroRepository livroRepository;
    private final AutorRepository autorRepository;
    private final LivroAutorRepository livroAutorRepository;
    private final GeneroRepository generoRepository;
    private final ItemBibliotecaRepository itemBibliotecaRepository;
    private final UsuarioAtual usuarioAtual;

    public AdicionarLivroService(
            BookSearchPort bookSearchPort,
            LivroRepository livroRepository,
            AutorRepository autorRepository,
            LivroAutorRepository livroAutorRepository,
            GeneroRepository generoRepository,
            ItemBibliotecaRepository itemBibliotecaRepository,
            UsuarioAtual usuarioAtual) {
        this.bookSearchPort = bookSearchPort;
        this.livroRepository = livroRepository;
        this.autorRepository = autorRepository;
        this.livroAutorRepository = livroAutorRepository;
        this.generoRepository = generoRepository;
        this.itemBibliotecaRepository = itemBibliotecaRepository;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional
    public AdicionarLivroResponse adicionar(AdicionarLivroRequest request) {
        String lista = validarLista(request.lista());
        if (request.volumeId() == null || request.volumeId().isBlank()) {
            throw new LivroNaoEncontradoException();
        }

        // Não confia no que o app manda: busca os detalhes do volume pelo catálogo.
        GoogleBookVolume volume =
                bookSearchPort
                        .getById(request.volumeId())
                        .orElseThrow(LivroNaoEncontradoException::new);
        VolumeInfo info = volume.volumeInfo();

        String isbn = isbnDe(info);
        String titulo = tituloDe(info);
        if (isbn == null || titulo == null || info.pageCount() == null || info.pageCount() <= 0) {
            throw new LivroSemIsbnOuPaginasException();
        }

        Livro livro = buscarOuCriarLivro(isbn, titulo, info);
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        switch (lista) {
            case "lido" -> definirStatus(usuarioId, livro.getId(), StatusLeitura.lido);
            case "desejo" -> definirStatus(usuarioId, livro.getId(), StatusLeitura.desejo);
            case "favorito" ->
                    itemBibliotecaRepository.marcarFavorito(
                            UUID.randomUUID(), usuarioId, livro.getId());
            default -> throw new ListaInvalidaException();
        }

        ItemBiblioteca item =
                itemBibliotecaRepository
                        .findByUsuarioIdAndLivroId(usuarioId, livro.getId())
                        .orElseThrow();

        return new AdicionarLivroResponse(
                livro.getId(),
                livro.getTitulo(),
                livro.getCapaUrl(),
                item.getStatusLeitura().name(),
                item.isFavorito());
    }

    private void definirStatus(UUID usuarioId, UUID livroId, StatusLeitura status) {
        itemBibliotecaRepository.definirStatus(
                UUID.randomUUID(), usuarioId, livroId, status.name());
    }

    private String validarLista(String lista) {
        if (lista == null || !LISTAS_VALIDAS.contains(lista.trim().toLowerCase(Locale.ROOT))) {
            throw new ListaInvalidaException();
        }
        return lista.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Título do catálogo, sem espaços nas pontas e cortado em 255 caracteres (a coluna). Conta em
     * pontos de código, para não partir um par substituto ao cortar. Sem título, devolve null.
     */
    private static String tituloDe(VolumeInfo info) {
        if (info == null || info.title() == null || info.title().isBlank()) {
            return null;
        }
        String titulo = info.title().trim();
        if (titulo.codePointCount(0, titulo.length()) <= TITULO_MAXIMO) {
            return titulo;
        }
        return titulo.substring(0, titulo.offsetByCodePoints(0, TITULO_MAXIMO));
    }

    /** Prefere o ISBN de 13 dígitos; sem ele, usa o de 10. */
    private String isbnDe(VolumeInfo info) {
        List<IndustryIdentifier> identificadores = info == null ? null : info.industryIdentifiers();
        if (identificadores == null) {
            return null;
        }

        String isbn13 =
                identificadores.stream()
                        .filter(Objects::nonNull)
                        .filter(i -> "ISBN_13".equals(i.type()))
                        .map(IndustryIdentifier::identifier)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(isbn -> !isbn.isBlank())
                        .findFirst()
                        .orElse(null);
        if (isbn13 != null) {
            return isbn13;
        }

        return identificadores.stream()
                .filter(Objects::nonNull)
                .filter(i -> "ISBN_10".equals(i.type()))
                .map(IndustryIdentifier::identifier)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(isbn -> !isbn.isBlank())
                .findFirst()
                .orElse(null);
    }

    /**
     * Reutiliza um livro existente quando o ISBN coincide ou quando o título e o primeiro autor são
     * equivalentes após normalização.
     *
     * <p>Se não encontrar correspondência, cria o livro normalmente.
     */
    private Livro buscarOuCriarLivro(String isbn, String titulo, VolumeInfo info) {
        Optional<Livro> livroPorIsbn = livroRepository.findByIsbn(isbn);

        if (livroPorIsbn.isPresent()) {
            return livroPorIsbn.get();
        }

        Optional<Livro> livroPorTituloEAutor = buscarPorTituloEAutor(titulo, info);

        if (livroPorTituloEAutor.isPresent()) {
            return livroPorTituloEAutor.get();
        }

        UUID novoId = UUID.randomUUID();
        livroRepository.criarSeAusente(novoId, isbn, titulo, info.pageCount(), capaDe(info));

        Livro livro = livroRepository.findByIsbn(isbn).orElseThrow();

        if (livro.getId().equals(novoId)) {
            vincularAutores(livro, info.authors());
            vincularGeneros(livro, info.categories());
        }

        return livro;
    }

    /**
     * Procura um livro pelo título e pelo primeiro autor. A comparação ignora acentos, pontuação,
     * espaços e maiúsculas.
     */
    private Optional<Livro> buscarPorTituloEAutor(String titulo, VolumeInfo info) {
        if (info.authors() == null || info.authors().isEmpty()) {
            return Optional.empty();
        }

        String tituloNormalizado = normalizarTexto(titulo);
        String primeiroAutorNormalizado = normalizarTexto(info.authors().get(0));

        if (tituloNormalizado.isEmpty() || primeiroAutorNormalizado.isEmpty()) {
            return Optional.empty();
        }

        return livroRepository.buscarLivrosComAutores().stream()
                .filter(livro -> tituloNormalizado.equals(normalizarTexto(livro.getTitulo())))
                .filter(livro -> !livro.getLivroAutores().isEmpty())
                .filter(
                        livro -> {
                            LivroAutor primeiroVinculo = livro.getLivroAutores().get(0);
                            Autor autor = primeiroVinculo.getAutor();

                            return autor != null
                                    && primeiroAutorNormalizado.equals(
                                            normalizarTexto(autor.getNome()));
                        })
                .findFirst();
    }

    /**
     * Normaliza textos removendo acentos, pontuação, espaços e diferenças entre letras maiúsculas e
     * minúsculas.
     */
    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }

        String textoSemAcentos =
                Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");

        return textoSemAcentos.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private void vincularAutores(Livro livro, List<String> autores) {
        if (autores == null) {
            return;
        }

        short ordem = 1;
        for (String nome : autores) {
            if (nome == null || nome.isBlank()) {
                continue;
            }

            Autor autor =
                    autorRepository
                            .findByNome(nome)
                            .orElseGet(
                                    () -> autorRepository.save(new Autor(UUID.randomUUID(), nome)));
            livroAutorRepository.save(new LivroAutor(livro, autor, ordem));
            ordem++;
        }
    }

    private void vincularGeneros(Livro livro, List<String> categorias) {
        if (categorias == null) {
            return;
        }

        for (String nome : categorias) {
            if (nome == null || nome.isBlank()) {
                continue;
            }

            generoRepository.criarSeAusente(UUID.randomUUID(), nome);
            generoRepository
                    .findByNome(nome)
                    .ifPresent(genero -> generoRepository.vincular(livro.getId(), genero.getId()));
        }
    }

    private String capaDe(VolumeInfo info) {
        if (info.imageLinks() == null) {
            return null;
        }
        return info.imageLinks().thumbnail() != null
                ? info.imageLinks().thumbnail()
                : info.imageLinks().smallThumbnail();
    }
}
