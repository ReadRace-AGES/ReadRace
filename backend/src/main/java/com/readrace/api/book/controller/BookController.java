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
 * independente de estar rodando contra o mock local ou a API real. O resultado é idêntico nos dois
 * ambientes.
 *
 * <p>A busca aceita filtros por campo específico (título, autor, gênero) OU uma busca livre. Quando
 * mais de um filtro é informado, todos devem ser satisfeitos (E lógico).
 *
 * <pre>
 * GET /api/books/volumes?title=senhor+dos+aneis          → busca por título
 * GET /api/books/volumes?author=tolkien                  → busca por autor
 * GET /api/books/volumes?genre=fantasia                  → busca por gênero
 * GET /api/books/volumes?author=rowling&genre=fantasia   → autor E gênero
 * GET /api/books/volumes?q=tolkien                       → busca livre (todos os campos)
 * GET /api/books/volumes/{volumeId}                      → busca por ID (200 | 404)
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
     * Busca livros por título, autor, gênero ou texto livre.
     *
     * <p>Pelo menos um dos parâmetros de busca ({@code title}, {@code author}, {@code genre} ou
     * {@code q}) deve ser informado.
     *
     * @param title termo a buscar no título (opcional)
     * @param author termo a buscar no autor (opcional)
     * @param genre termo a buscar no gênero/categoria (opcional)
     * @param q busca livre em todos os campos (opcional)
     * @param maxResults máximo de resultados (1-40, padrão 10)
     * @param startIndex índice do primeiro resultado (padrão 0)
     */
    @GetMapping
    public GoogleBooksResponse search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer maxResults,
            @RequestParam(required = false) Integer startIndex) {
        return bookService.search(title, author, genre, q, maxResults, startIndex);
    }

    /** Busca um volume específico pelo ID. */
    @GetMapping("/{volumeId}")
    public GoogleBookVolume getById(@PathVariable String volumeId) {
        return bookService.getById(volumeId);
    }
}
