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
import com.readrace.api.book.dto.VolumeInfo;
import com.readrace.api.book.port.BookSearchPort;

/**
 * Implementação local (mock) do {@link BookSearchPort}.
 *
 * <p>Carrega os livros de um arquivo JSON em memória ao iniciar a aplicação e simula o
 * comportamento de busca da Google Books API, incluindo os qualificadores de campo ({@code
 * intitle:}, {@code inauthor:}, {@code subject:}), paginação e busca por ID.
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

        SearchQuery searchQuery = SearchQuery.parse(query);

        List<GoogleBookVolume> matched =
                books.stream().filter(book -> matches(book, searchQuery)).toList();

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
     * Aplica a query ao livro. Se a busca usa qualificadores ({@code intitle}, {@code inauthor},
     * {@code subject}), todos precisam bater (E lógico), reproduzindo o comportamento do Google
     * Books. Se é busca geral, o termo pode aparecer em qualquer campo.
     */
    private boolean matches(GoogleBookVolume book, SearchQuery searchQuery) {
        VolumeInfo info = book.volumeInfo();
        if (info == null) {
            return false;
        }

        if (searchQuery.isGeneralSearch()) {
            return matchesGeneral(info, searchQuery.freeText());
        }

        return matchesTitle(info, searchQuery.titleTerms())
                && matchesAuthor(info, searchQuery.authorTerms())
                && matchesSubject(info, searchQuery.subjectTerms())
                && matchesGeneral(info, searchQuery.freeText());
    }

    /** Busca geral: o termo aparece em qualquer campo relevante. */
    private boolean matchesGeneral(VolumeInfo info, String freeText) {
        if (freeText == null || freeText.isBlank()) {
            return true;
        }

        String term = normalize(freeText);

        if (containsNormalized(info.title(), term)) {
            return true;
        }
        if (anyContains(info.authors(), term)) {
            return true;
        }
        if (anyContains(info.categories(), term)) {
            return true;
        }
        if (containsNormalized(info.description(), term)) {
            return true;
        }
        return matchesIsbn(info, term);
    }

    /** Todos os termos de título precisam aparecer no título. */
    private boolean matchesTitle(VolumeInfo info, List<String> titleTerms) {
        return titleTerms.stream()
                .allMatch(term -> containsNormalized(info.title(), normalize(term)));
    }

    /** Todos os termos de autor precisam aparecer em algum autor. */
    private boolean matchesAuthor(VolumeInfo info, List<String> authorTerms) {
        return authorTerms.stream().allMatch(term -> anyContains(info.authors(), normalize(term)));
    }

    /** Todos os termos de gênero precisam aparecer em alguma categoria. */
    private boolean matchesSubject(VolumeInfo info, List<String> subjectTerms) {
        return subjectTerms.stream()
                .allMatch(term -> anyContains(info.categories(), normalize(term)));
    }

    /** Verifica se o termo aparece em algum ISBN. */
    private boolean matchesIsbn(VolumeInfo info, String normalizedTerm) {
        if (info.industryIdentifiers() == null) {
            return false;
        }
        return info.industryIdentifiers().stream()
                .anyMatch(
                        identifier -> containsNormalized(identifier.identifier(), normalizedTerm));
    }

    /** Verifica se algum item da lista contém o termo normalizado. */
    private boolean anyContains(List<String> values, String normalizedTerm) {
        if (values == null) {
            return false;
        }
        return values.stream().anyMatch(value -> containsNormalized(value, normalizedTerm));
    }

    /** Compara textos ignorando acentos e case. */
    private boolean containsNormalized(String text, String normalizedTerm) {
        if (text == null) {
            return false;
        }
        return normalize(text).contains(normalizedTerm);
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
    private record SeedEntry(String id, VolumeInfo volumeInfo) {}
}
