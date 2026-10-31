package com.readrace.api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resposta de GET /api/clubes/{clubeId}/posts:
 * posts raiz do clube, acompanhados do cabeçalho.
 */
public record ForumClubeResponse(Clube clube, List<Post> posts, boolean podeEscrever) {

    public record Clube(UUID id, String nome, LivroAtual livroAtual) {}

    public record LivroAtual(String titulo, String autor, String capaUrl) {}

    /**
     * totalComentarios inclui comentários e respostas da thread.
     * curtidoPorMim indica se o usuário atual curtiu o post.
     */
    public record Post(
            UUID id,
            Autor autor,
            OffsetDateTime publicadoEm,
            String texto,
            long totalCurtidas,
            boolean curtidoPorMim,
            long totalComentarios) {}

    public record Autor(UUID id, String nome, String avatarUrl, Integer sequenciaDias) {}
}