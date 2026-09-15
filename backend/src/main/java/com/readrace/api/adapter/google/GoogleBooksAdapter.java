package com.readrace.api.adapter.google;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.readrace.api.dto.GoogleBookVolume;
import com.readrace.api.dto.GoogleBooksResponse;
import com.readrace.api.exception.ServicoExternoIndisponivelException;
import com.readrace.api.service.BookSearchPort;

/**
 * Implementação real do {@link BookSearchPort} que consome a Google Books API.
 *
 * <p>Ativado apenas com o profile {@code prod}. Requer a variável de ambiente {@code
 * GOOGLE_BOOKS_API_KEY} configurada.
 *
 * <p>Endpoint base: {@code https://www.googleapis.com/books/v1/volumes}
 *
 * <p>Limites do tier gratuito: 1000 requests/dia.
 *
 * <p>Toda chamada externa tem timeout de conexão e de leitura, e qualquer falha da API do Google
 * vira {@link ServicoExternoIndisponivelException} (HTTP 503, {@code code =
 * EXTERNAL_SERVICE_UNAVAILABLE}) — nunca vaza como {@code 500}, que sugeriria erro nosso. Assim o
 * cliente distingue "Google fora do ar" de um problema interno.
 */
@Component
@Profile("prod")
public class GoogleBooksAdapter implements BookSearchPort {

    private static final Logger log = LoggerFactory.getLogger(GoogleBooksAdapter.class);
    private static final String BASE_URL = "https://www.googleapis.com/books/v1/volumes";

    // Google costuma responder em poucos segundos; se passar disso, é melhor falhar rápido
    // do que segurar a thread e propagar lentidão para o cliente.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient restClient;
    private final String apiKey;

    public GoogleBooksAdapter(@Value("${google.books.api-key}") String apiKey) {
        this.restClient =
                RestClient.builder().baseUrl(BASE_URL).requestFactory(requestFactory()).build();
        this.apiKey = apiKey;
        log.info("GoogleBooksAdapter inicializado (API key configurada)");
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    @Override
    public GoogleBooksResponse search(String query, int maxResults, int startIndex) {
        URI uri = montarUrlBusca(query, maxResults, startIndex, apiKey);

        try {
            GoogleBooksResponse response =
                    restClient.get().uri(uri).retrieve().body(GoogleBooksResponse.class);

            return response != null ? response : GoogleBooksResponse.empty();
        } catch (RestClientException e) {
            throw indisponivel(e);
        }
    }

    @Override
    public Optional<GoogleBookVolume> getById(String volumeId) {
        URI uri = montarUrlPorId(volumeId, apiKey);

        try {
            GoogleBookVolume volume =
                    restClient
                            .get()
                            .uri(uri)
                            // 404 do Google não é erro nosso: significa "não existe".
                            .retrieve()
                            .onStatus(
                                    status -> status.value() == HttpStatus.NOT_FOUND.value(),
                                    (request, response) -> {
                                        throw new VolumeNaoEncontrado();
                                    })
                            .body(GoogleBookVolume.class);

            return Optional.ofNullable(volume);
        } catch (VolumeNaoEncontrado e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw indisponivel(e);
        }
    }

    /**
     * Monta a URL ABSOLUTA de busca a partir do {@link #BASE_URL} (não um caminho relativo).
     *
     * <p>Com caminho relativo + {@code baseUrl}, o RestClient aplicaria a regra de resolução de URL
     * relativa da RFC 3986 e descartaria o último segmento — sairia {@code .../books/v1/} em vez de
     * {@code .../books/v1/volumes}, quebrando toda busca em produção. Partindo do {@code BASE_URL}
     * absoluto, não há resolução relativa.
     *
     * <p>O {@code .encode()} garante que caracteres reservados no valor ({@code &}, {@code #} —
     * ex.: "Tom &amp; Jerry", "C#") sejam percent-encodados e não cortem a query nem a {@code key}.
     *
     * <p>Package-private para ser testável sem rede nem chave real: o teste chama este método e
     * compara a URL montada com a esperada.
     */
    static URI montarUrlBusca(String query, int maxResults, int startIndex, String apiKey) {
        return UriComponentsBuilder.fromUriString(BASE_URL)
                .queryParam("q", query)
                .queryParam("maxResults", maxResults)
                .queryParam("startIndex", startIndex)
                .queryParam("key", apiKey)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
    }

    /** Monta a URL ABSOLUTA {@code BASE_URL/{id}} (ver {@link #montarUrlBusca}). */
    static URI montarUrlPorId(String volumeId, String apiKey) {
        return UriComponentsBuilder.fromUriString(BASE_URL)
                .pathSegment("{id}")
                .queryParam("key", apiKey)
                .encode(StandardCharsets.UTF_8)
                .buildAndExpand(volumeId)
                .toUri();
    }

    private ServicoExternoIndisponivelException indisponivel(Exception causa) {
        log.warn("Falha ao consultar a Google Books API", causa);
        return new ServicoExternoIndisponivelException(
                "Serviço de livros indisponível no momento.");
    }

    /** Sinaliza internamente que o Google respondeu 404, para virar Optional.empty(). */
    private static class VolumeNaoEncontrado extends RuntimeException {}
}
