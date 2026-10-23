package com.readrace.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

@DisplayName("ImageLinks")
class ImageLinksTest {

    private static final String CAPA_HTTP =
            "http://books.google.com/books/content?id=abc&printsec=frontcover&img=1&zoom=1";
    private static final String CAPA_HTTPS =
            "https://books.google.com/books/content?id=abc&printsec=frontcover&img=1&zoom=1";

    @Test
    void deve_trocar_http_por_https_ao_ler_o_json_do_google_books() throws Exception {
        String json =
                """
                {"smallThumbnail": "%s", "thumbnail": "%s"}
                """
                        .formatted(CAPA_HTTP, CAPA_HTTP);

        ImageLinks links = JsonMapper.builder().build().readValue(json, ImageLinks.class);

        assertThat(links.thumbnail()).isEqualTo(CAPA_HTTPS);
        assertThat(links.smallThumbnail()).isEqualTo(CAPA_HTTPS);
    }

    @Test
    void deve_manter_https_e_nulos_como_estao() {
        ImageLinks links = new ImageLinks(null, CAPA_HTTPS);

        assertThat(links.thumbnail()).isEqualTo(CAPA_HTTPS);
        assertThat(links.smallThumbnail()).isNull();
        assertThat(links.large()).isNull();
    }
}
