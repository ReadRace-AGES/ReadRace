package com.readrace.api.book.dto;

/**
 * Representa um volume (livro) individual retornado pela Google Books API.
 *
 * <p>Espelha exatamente o JSON retornado por {@code GET /volumes/{id}}.
 */
public record GoogleBookVolume(
        String kind,
        String id,
        String etag,
        String selfLink,
        VolumeInfo volumeInfo,
        SaleInfo saleInfo,
        AccessInfo accessInfo) {

    /** Cria um volume com os campos obrigatórios, defaults de sale e access. */
    public static GoogleBookVolume of(String id, VolumeInfo volumeInfo) {
        return new GoogleBookVolume(
                "books#volume",
                id,
                id,
                "https://www.googleapis.com/books/v1/volumes/" + id,
                volumeInfo,
                SaleInfo.notForSale(),
                AccessInfo.noAccess());
    }
}
