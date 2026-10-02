package com.readrace.api.dto.response;

/**
 * Resposta de {@code GET /api/biblioteca}: a primeira página de cada uma das quatro listas da aba
 * Meus Livros, cada uma na ordem de atividade mais recente. Um favorito também aparece na lista do
 * seu estado de leitura. Não existe campo de recomendações (#30).
 */
public record BibliotecaResponse(
        PaginaBibliotecaResponse favoritos,
        PaginaBibliotecaResponse lendo,
        PaginaBibliotecaResponse desejo,
        PaginaBibliotecaResponse lidos) {}
