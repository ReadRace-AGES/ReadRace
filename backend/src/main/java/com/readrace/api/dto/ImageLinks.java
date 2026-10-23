package com.readrace.api.dto;

/**
 * Links de imagem da capa do livro em diferentes tamanhos.
 *
 * <p>Espelha o campo {@code volumeInfo.imageLinks} da Google Books API.
 */
public record ImageLinks(
        String smallThumbnail,
        String thumbnail,
        String small,
        String medium,
        String large,
        String extraLarge) {

    /**
     * O Google Books devolve as capas em {@code http://}, e o Android bloqueia {@code http} no app
     * instalado (APK): a capa não aparecia. O Google serve as mesmas imagens em {@code https}, e
     * trocar aqui vale para a busca e para o livro salvo.
     */
    public ImageLinks {
        smallThumbnail = comHttps(smallThumbnail);
        thumbnail = comHttps(thumbnail);
        small = comHttps(small);
        medium = comHttps(medium);
        large = comHttps(large);
        extraLarge = comHttps(extraLarge);
    }

    private static String comHttps(String url) {
        return url != null && url.startsWith("http://") ? "https://" + url.substring(7) : url;
    }

    /** Construtor de conveniência quando só se tem thumbnail. */
    public ImageLinks(String smallThumbnail, String thumbnail) {
        this(smallThumbnail, thumbnail, null, null, null, null);
    }
}
