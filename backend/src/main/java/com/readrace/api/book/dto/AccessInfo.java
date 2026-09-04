package com.readrace.api.book.dto;

/**
 * Informações de acesso ao volume.
 *
 * <p>Espelha o campo {@code accessInfo} da Google Books API. No mock, retorna valores estáticos
 * pois o ReadRace não oferece leitura dentro da plataforma.
 */
public record AccessInfo(
        String country, String viewability, boolean embeddable, boolean publicDomain) {

    /** Valor padrão para livros sem acesso de leitura. */
    public static AccessInfo noAccess() {
        return new AccessInfo("BR", "NO_PAGES", false, false);
    }
}
