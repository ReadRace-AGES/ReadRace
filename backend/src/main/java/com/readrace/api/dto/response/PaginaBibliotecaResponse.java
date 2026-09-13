package com.readrace.api.dto.response;

import java.util.List;

/**
 * Uma página de uma lista da biblioteca. {@code proximoCursor} nulo significa que a lista acabou;
 * caso contrário, vai em {@code GET /api/biblioteca/{lista}?cursor=} para buscar a seguinte.
 */
public record PaginaBibliotecaResponse(List<LivroBibliotecaResponse> itens, String proximoCursor) {

    public static PaginaBibliotecaResponse vazia() {
        return new PaginaBibliotecaResponse(List.of(), null);
    }
}
