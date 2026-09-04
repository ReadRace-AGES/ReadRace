package com.readrace.api.book.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.readrace.api.book.dto.GoogleBookVolume;
import com.readrace.api.book.dto.GoogleBooksResponse;
import com.readrace.api.book.port.BookSearchPort;
import com.readrace.api.exception.RecursoNaoEncontradoException;

/**
 * Serviço de busca de livros.
 *
 * <p>Delega para o {@link BookSearchPort} ativo (mock local ou Google Books API). Monta a query no
 * formato entendido pela Google Books API a partir dos filtros de título, autor e gênero,
 * garantindo que o mock local e a API real produzam resultados idênticos.
 */
@Service
public class BookService {

    private static final int MAX_RESULTS_DEFAULT = 10;
    private static final int MAX_RESULTS_LIMIT = 40;

    private final BookSearchPort bookSearchPort;

    public BookService(BookSearchPort bookSearchPort) {
        this.bookSearchPort = bookSearchPort;
    }

    /**
     * Busca livros combinando filtros de título, autor, gênero e/ou texto livre.
     *
     * <p>Monta uma query no formato da Google Books API usando os qualificadores {@code intitle:},
     * {@code inauthor:} e {@code subject:}. Assim, tanto o mock local quanto a API real recebem a
     * mesma query e retornam resultados equivalentes.
     *
     * @param title termo de título (opcional)
     * @param author termo de autor (opcional)
     * @param genre termo de gênero/categoria (opcional)
     * @param isbn ISBN do livro (opcional)
     * @param freeText busca livre em todos os campos (opcional)
     * @param maxResults máximo de resultados (1-40, padrão 10)
     * @param startIndex índice do primeiro resultado (padrão 0)
     * @return resposta no formato Google Books API
     */
    public GoogleBooksResponse search(
            String title,
            String author,
            String genre,
            String isbn,
            String freeText,
            Integer maxResults,
            Integer startIndex) {
        String query = buildQuery(title, author, genre, isbn, freeText);

        if (query.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe ao menos um filtro de busca: title, author, genre, isbn ou q");
        }

        int limit = clamp(maxResults, 1, MAX_RESULTS_LIMIT, MAX_RESULTS_DEFAULT);
        int offset = (startIndex != null && startIndex >= 0) ? startIndex : 0;

        return bookSearchPort.search(query, limit, offset);
    }

    /**
     * Busca um volume específico pelo ID.
     *
     * @param volumeId identificador do volume
     * @return o volume encontrado
     * @throws RecursoNaoEncontradoException se não existir
     */
    public GoogleBookVolume getById(String volumeId) {
        GoogleBookVolume volume = bookSearchPort.getById(volumeId);

        if (volume == null) {
            throw new RecursoNaoEncontradoException(
                    "Volume '%s' não encontrado".formatted(volumeId));
        }

        return volume;
    }

    /**
     * Monta a query no formato Google Books a partir dos filtros. Ex: title="senhor",
     * author="tolkien" vira {@code intitle:"senhor" inauthor:"tolkien"}.
     */
    private String buildQuery(
            String title, String author, String genre, String isbn, String freeText) {
        List<String> parts = new ArrayList<>();

        if (isNotBlank(title)) {
            parts.add("intitle:\"%s\"".formatted(title.trim()));
        }
        if (isNotBlank(author)) {
            parts.add("inauthor:\"%s\"".formatted(author.trim()));
        }
        if (isNotBlank(genre)) {
            parts.add("subject:\"%s\"".formatted(genre.trim()));
        }
        if (isNotBlank(isbn)) {
            // Remove hífens e espaços do ISBN (formato usado pela Google Books API)
            parts.add("isbn:%s".formatted(isbn.trim().replaceAll("[\\s-]", "")));
        }
        if (isNotBlank(freeText)) {
            parts.add(freeText.trim());
        }

        return String.join(" ", parts);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    /** Garante que o valor está entre min e max, usando defaultValue se for null. */
    private int clamp(Integer value, int min, int max, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Math.max(min, Math.min(max, value));
    }
}
