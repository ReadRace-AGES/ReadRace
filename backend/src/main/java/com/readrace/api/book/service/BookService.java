package com.readrace.api.book.service;

import org.springframework.stereotype.Service;

import com.readrace.api.book.dto.GoogleBookVolume;
import com.readrace.api.book.dto.GoogleBooksResponse;
import com.readrace.api.book.port.BookSearchPort;
import com.readrace.api.exception.RecursoNaoEncontradoException;

/**
 * Serviço de busca de livros.
 *
 * <p>Delega para o {@link BookSearchPort} ativo (mock local ou Google Books API). Centraliza
 * validações e regras que não pertencem ao adapter nem ao controller.
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
     * Busca livros por texto livre.
     *
     * @param query termo de busca (obrigatório)
     * @param maxResults máximo de resultados (1-40, padrão 10)
     * @param startIndex índice do primeiro resultado (padrão 0)
     * @return resposta no formato Google Books API
     */
    public GoogleBooksResponse search(String query, Integer maxResults, Integer startIndex) {
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

    /** Garante que o valor está entre min e max, usando defaultValue se for null. */
    private int clamp(Integer value, int min, int max, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Math.max(min, Math.min(max, value));
    }
}
