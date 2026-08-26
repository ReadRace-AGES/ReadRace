package com.readrace.api.book.adapter.google;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.readrace.api.book.dto.GoogleBookVolume;
import com.readrace.api.book.dto.GoogleBooksResponse;
import com.readrace.api.book.port.BookSearchPort;

/**
 * Implementação real do {@link BookSearchPort} que consome a Google Books API.
 *
 * <p>Ativado apenas com o profile {@code prod}. Requer a variável de ambiente {@code
 * GOOGLE_BOOKS_API_KEY} configurada.
 *
 * <p>Endpoint base: {@code https://www.googleapis.com/books/v1/volumes}
 *
 * <p>Limites do tier gratuito: 1000 requests/dia.
 */
@Component
@Profile("prod")
public class GoogleBooksAdapter implements BookSearchPort {

    private static final Logger log = LoggerFactory.getLogger(GoogleBooksAdapter.class);
    private static final String BASE_URL = "https://www.googleapis.com/books/v1/volumes";

    private final RestClient restClient;
    private final String apiKey;

    public GoogleBooksAdapter(@Value("${google.books.api-key}") String apiKey) {
        this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
        this.apiKey = apiKey;
        log.info("GoogleBooksAdapter inicializado (API key configurada)");
    }

    @Override
    public GoogleBooksResponse search(String query, int maxResults, int startIndex) {
        String uri =
                UriComponentsBuilder.fromPath("")
                        .queryParam("q", query)
                        .queryParam("maxResults", maxResults)
                        .queryParam("startIndex", startIndex)
                        .queryParam("key", apiKey)
                        .build()
                        .toUriString();

        GoogleBooksResponse response =
                restClient.get().uri(uri).retrieve().body(GoogleBooksResponse.class);

        if (response == null) {
            return GoogleBooksResponse.empty();
        }

        return response;
    }

    @Override
    public GoogleBookVolume getById(String volumeId) {
        String uri =
                UriComponentsBuilder.fromPath("/{id}")
                        .queryParam("key", apiKey)
                        .buildAndExpand(volumeId)
                        .toUriString();

        return restClient.get().uri(uri).retrieve().body(GoogleBookVolume.class);
    }
}
