package com.readrace.api.service;

import java.util.List;
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

        // Não confia no que o app manda: busca os detalhes do volume pelo catálogo.
        GoogleBookVolume volume =
                bookSearchPort
                        .getById(request.volumeId())
                        .orElseThrow(LivroNaoEncontradoException::new);
        VolumeInfo info = volume.volumeInfo();

        String isbn = isbnDe(info);
        if (isbn == null || info.pageCount() == null || info.pageCount() <= 0) {
            throw new LivroSemIsbnOuPaginasException();
        }

        Livro livro = buscarOuCriarLivro(isbn, info);
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
        if (lista == null || !LISTAS_VALIDAS.contains(lista.trim().toLowerCase())) {
            throw new ListaInvalidaException();
        }
        return lista.trim().toLowerCase();
    }

    /** Prefere o ISBN de 13 dígitos; sem ele, usa o de 10. */
    private String isbnDe(VolumeInfo info) {
        List<IndustryIdentifier> identificadores = info.industryIdentifiers();
        if (identificadores == null) {
            return null;
        }

        String isbn13 =
                identificadores.stream()
                        .filter(i -> "ISBN_13".equals(i.type()))
                        .map(IndustryIdentifier::identifier)
                        .findFirst()
                        .orElse(null);
        if (isbn13 != null) {
            return isbn13;
        }

        return identificadores.stream()
                .filter(i -> "ISBN_10".equals(i.type()))
                .map(IndustryIdentifier::identifier)
                .findFirst()
                .orElse(null);
    }

    /**
     * O ISBN identifica o livro entre usuários: se já existe, reaproveita (autores e gêneros já
     * foram vinculados por quem criou primeiro). Só cria autores/gêneros quando a própria chamada
     * cria a linha do livro, para não recriar vínculos de um livro já existente.
     */
    private Livro buscarOuCriarLivro(String isbn, VolumeInfo info) {
        return livroRepository
                .findByIsbn(isbn)
                .orElseGet(
                        () -> {
                            UUID novoId = UUID.randomUUID();
                            livroRepository.criarSeAusente(
                                    novoId, isbn, info.title(), info.pageCount(), capaDe(info));
                            Livro livro = livroRepository.findByIsbn(isbn).orElseThrow();

                            if (livro.getId().equals(novoId)) {
                                vincularAutores(livro, info.authors());
                                vincularGeneros(livro, info.categories());
                            }

                            return livro;
                        });
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
