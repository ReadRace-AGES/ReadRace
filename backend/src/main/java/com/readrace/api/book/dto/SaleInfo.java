package com.readrace.api.book.dto;

/**
 * Informações de venda do volume.
 *
 * <p>Espelha o campo {@code saleInfo} da Google Books API. No mock, retorna valores estáticos pois
 * o ReadRace não lida com venda de livros.
 */
public record SaleInfo(String country, String saleability, boolean isEbook) {

    /** Valor padrão para livros não vendidos via Google. */
    public static SaleInfo notForSale() {
        return new SaleInfo("BR", "NOT_FOR_SALE", false);
    }
}
