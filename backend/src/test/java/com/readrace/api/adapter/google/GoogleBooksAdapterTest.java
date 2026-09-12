package com.readrace.api.adapter.google;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testa a MONTAGEM da URL do {@link GoogleBooksAdapter} sem rede e sem chave real.
 *
 * <p>O adapter só roda em prod (não sobe no contexto de teste), mas foi onde os bugs de URL
 * apareceram. Como a montagem da URL é determinística, dá para capturar a URL que iria para o
 * Google e conferir os dois pontos que quebraram:
 *
 * <ul>
 *   <li>o segmento {@code /volumes} do caminho não pode ser descartado (regressão da resolução
 *       relativa que fazia toda busca falhar e todo id virar 404);
 *   <li>{@code &} e {@code #} no valor precisam sair percent-encodados, sem cortar a query nem a
 *       {@code key}.
 * </ul>
 */
@DisplayName("GoogleBooksAdapter — montagem da URL")
class GoogleBooksAdapterTest {

    private static final String BASE = "https://www.googleapis.com/books/v1/volumes";

    @Test
    void busca_deve_preservar_o_caminho_volumes() {
        URI uri = GoogleBooksAdapter.montarUrlBusca("java", 10, 0, "KEY");

        assertThat(uri.toString()).startsWith(BASE + "?");
        assertThat(uri.getPath()).isEqualTo("/books/v1/volumes");
    }

    @Test
    void busca_deve_incluir_todos_os_parametros() {
        URI uri = GoogleBooksAdapter.montarUrlBusca("java", 5, 20, "KEY");

        assertThat(uri.getRawQuery())
                .contains("q=java")
                .contains("maxResults=5")
                .contains("startIndex=20")
                .contains("key=KEY");
    }

    @Test
    void busca_deve_encodar_e_comercial_sem_cortar_a_query_nem_a_key() {
        URI uri = GoogleBooksAdapter.montarUrlBusca("Tom & Jerry", 10, 0, "KEY");

        // & do valor vira %26; não pode aparecer como separador extra dentro do q.
        assertThat(uri.getRawQuery()).contains("%26").contains("key=KEY");
        // A key continua presente e íntegra após decodificar.
        assertThat(uri.getQuery()).contains("Tom & Jerry").contains("key=KEY");
    }

    @Test
    void busca_deve_encodar_cerquilha_sem_virar_fragmento() {
        URI uri = GoogleBooksAdapter.montarUrlBusca("C#", 10, 0, "KEY");

        // # do valor vira %23; nada pode virar fragmento (senão a key sumiria).
        assertThat(uri.getRawQuery()).contains("%23").contains("key=KEY");
        assertThat(uri.getFragment()).isNull();
        assertThat(uri.getPath()).isEqualTo("/books/v1/volumes");
    }

    @Test
    void por_id_deve_preservar_o_caminho_volumes_e_anexar_o_id() {
        URI uri = GoogleBooksAdapter.montarUrlPorId("sci001", "KEY");

        assertThat(uri.getPath()).isEqualTo("/books/v1/volumes/sci001");
        assertThat(uri.getRawQuery()).contains("key=KEY");
    }
}
