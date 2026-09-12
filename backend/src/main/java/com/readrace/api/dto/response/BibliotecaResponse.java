package com.readrace.api.dto.response;

import java.util.List;

/**
 * Resposta de {@code GET /api/biblioteca}: as quatro listas que a aba Meus Livros exibe, cada uma
 * na ordem de atividade mais recente. Um favorito também aparece na lista do seu estado de leitura.
 * Não existe campo de recomendações (#30).
 */
public record BibliotecaResponse(
        List<LivroBibliotecaResponse> favoritos,
        List<LivroBibliotecaResponse> lendo,
        List<LivroBibliotecaResponse> desejo,
        List<LivroBibliotecaResponse> lidos) {}
