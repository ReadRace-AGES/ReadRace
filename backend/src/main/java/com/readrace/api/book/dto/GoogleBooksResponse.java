package com.readrace.api.book.dto;

import java.util.List;

/**
 * Resposta de busca da Google Books API.
 *
 * <p>Espelha exatamente o JSON retornado por {@code GET /volumes?q=...}.
 *
 * <pre>
 * {
 *   "kind": "books#volumes",
 *   "totalItems": 42,
 *   "items": [ ...volumes... ]
 * }
 * </pre>
 */
public record GoogleBooksResponse(String kind, int totalItems, List<GoogleBookVolume> items) {

    /** Resposta vazia (nenhum resultado encontrado). */
    public static GoogleBooksResponse empty() {
        return new GoogleBooksResponse("books#volumes", 0, List.of());
    }

    /** Cria uma resposta com a lista de itens e total. */
    public static GoogleBooksResponse of(List<GoogleBookVolume> items, int totalItems) {
        return new GoogleBooksResponse("books#volumes", totalItems, items);
    }
}
