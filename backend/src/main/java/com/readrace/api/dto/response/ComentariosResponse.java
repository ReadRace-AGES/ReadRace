package com.readrace.api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resposta de {@code GET /api/posts/{postId}/comentarios} (#150): a thread do post, em árvore. Cada
 * nível vem do mais antigo para o mais novo. {@code podeComentar} é {@code true} para quem segue a
 * comunidade do post raiz.
 */
public record ComentariosResponse(List<Comentario> comentarios, boolean podeComentar) {

    /**
     * Também é o corpo do {@code 201} de {@code POST /api/posts/{postId}/comentarios}, com {@code
     * respostas} vazio. Curtir usa a mesma rota do post: {@code /api/posts/{id}/curtida}.
     */
    public record Comentario(
            UUID id,
            String texto,
            OffsetDateTime publicadoEm,
            Autor autor,
            long totalCurtidas,
            boolean curtidoPorMim,
            List<Comentario> respostas) {}

    public record Autor(UUID id, String nome, String avatarUrl) {}
}
