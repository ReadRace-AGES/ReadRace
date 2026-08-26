package com.readrace.api.book.adapter.local;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readrace.api.book.dto.GoogleBookVolume;
import com.readrace.api.book.dto.GoogleBooksResponse;
import com.readrace.api.book.port.BookSearchPort;

/**
 * Implementação local (mock) do {@link BookSearchPort}.
 *
 * <p>Carrega ~60 livros de um arquivo JSON em memória ao iniciar a aplicação e simula o
 * comportamento de busca da Google Books API: filtragem por texto, paginação e busca por ID.
 *
 * <p>Ativado apenas com o profile {@code dev}. Em produção, o {@code GoogleBooksAdapter} é usado.
 */
@Component
@Profile("dev")
public class LocalBooksAdapter implements BookSearchPort {

    private static final Logger log = LoggerFactory.getLogger(LocalBooksAdapter.class);

    private final ObjectMapper objectMapper;

    private List<GoogleBookVolume> books;
    private Map<String, GoogleBookVolume> booksById;

    public LocalBooksAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadBooks() {
        try {
            InputStream input = new ClassPathResource("books-seed.json").getInputStream();

            List<SeedEntry> entries =
                    objectMapper.readValue(input, new TypeReference<List<SeedEntry>>() {});

            books =
                    entries.stream()
                            .map(entry -> GoogleBookVolume.of(entry.id(), entry.volumeInfo()))
                            .toList();

            booksById =
                    books.stream()
                            .collect(Collectors.toMap(GoogleBookVolume::id, Function.identity()));

            log.info("LocalBooksAdapter carregou {} livros do seed", books.size());
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao carregar books-seed.json", e);
        }
    }

    @Override
    public GoogleBooksResponse search(String query, int maxResults, int startIndex) {
        if (query == null || query.isBlank()) {
            return GoogleBooksResponse.empty();
        }

        String normalizedQuery = normalize(query);

        List<GoogleBookVolume> matched =
                books.stream().filter(book -> matchesQuery(book, normalizedQuery)).toList();

        int totalItems = matched.size();

        if (startIndex >= totalItems) {
            return GoogleBooksResponse.of(List.of(), totalItems);
        }

        int end = Math.min(startIndex + maxResults, totalItems);
        List<GoogleBookVolume> page = matched.subList(startIndex, end);

        return GoogleBooksResponse.of(page, totalItems);
    }

    @Override
    public GoogleBookVolume getById(String volumeId) {
        if (volumeId == null) {
            return null;
        }
        return booksById.get(volumeId);
    }

    /**
     * Simula a busca full-text da Google Books: verifica se o query aparece no título, autores,
     * categorias, ISBN ou descrição.
     */
    private boolean matchesQuery(GoogleBookVolume book, String normalizedQuery) {
        var info = book.volumeInfo();
        if (info == null) {
            return false;
        }

        if (containsNormalized(info.title(), normalizedQuery)) {
            return true;
        }

        if (info.authors() != null
                && info.authors().stream()
                        .anyMatch(author -> containsNormalized(author, normalizedQuery))) {
            return true;
        }

        if (info.categories() != null
                && info.categories().stream()
                        .anyMatch(category -> containsNormalized(category, normalizedQuery))) {
            return true;
        }

        if (info.industryIdentifiers() != null
                && info.industryIdentifiers().stream()
                        .anyMatch(
                                identifier ->
                                        containsNormalized(
                                                identifier.identifier(), normalizedQuery))) {
            return true;
        }

        if (containsNormalized(info.description(), normalizedQuery)) {
            return true;
        }

        return false;
    }

    /** Compara textos ignorando acentos e case. */
    private boolean containsNormalized(String text, String normalizedQuery) {
        if (text == null) {
            return false;
        }
        return normalize(text).contains(normalizedQuery);
    }

    /** Remove acentos e converte para minúsculas. */
    private String normalize(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").toLowerCase();
    }

    /**
     * Record auxiliar para desserializar o JSON de seed. O seed tem formato simplificado: apenas
     * {@code id} e {@code volumeInfo}.
     */
    private record SeedEntry(String id, com.readrace.api.book.dto.VolumeInfo volumeInfo) {}
}
