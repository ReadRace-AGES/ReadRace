package com.readrace.api.book.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.book.dto.GoogleBookVolume;
import com.readrace.api.book.dto.GoogleBooksResponse;
import com.readrace.api.book.service.BookService;

/**
 * Endpoints de busca de livros.
 *
 * <p>Espelha o comportamento da Google Books API para que o frontend use a mesma interface
 * independente de estar rodando contra o mock local ou a API real.
 *
 * <pre>
 * GET /api/books/volumes?q={query}&maxResults={n}&startIndex={n}   200 | 400
 * GET /api/books/volumes/{volumeId}                                 200 | 404
 * </pre>
 */
@RestController
@RequestMapping("/api/books/volumes")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    /**
     * Busca livros por texto livre.
     *
     * @param query termo de busca (obrigatório)
     * @param maxResults máximo de resultados (1-40, padrão 10)
     * @param startIndex índice do primeiro resultado (padrão 0)
     */
    @GetMapping
    public GoogleBooksResponse search(
            @RequestParam("q") String query,
            @RequestParam(required = false) Integer maxResults,
            @RequestParam(required = false) Integer startIndex) {
        return bookService.search(query, maxResults, startIndex);
    }

    /** Busca um volume específico pelo ID. */
    @GetMapping("/{volumeId}")
    public GoogleBookVolume getById(@PathVariable String volumeId) {
        return bookService.getById(volumeId);
    }
}
