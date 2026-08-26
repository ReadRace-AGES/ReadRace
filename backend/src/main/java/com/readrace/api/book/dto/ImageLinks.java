package com.readrace.api.book.dto;

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

    /** Construtor de conveniência quando só se tem thumbnail. */
    public ImageLinks(String smallThumbnail, String thumbnail) {
        this(smallThumbnail, thumbnail, null, null, null, null);
    }
}
