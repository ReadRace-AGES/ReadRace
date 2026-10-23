package com.readrace.api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ComunidadeResponse(
        UUID id, String nome, String imagemUrl, long seguidores, boolean sigo, List<Post> posts) {

    public record Post(
            UUID id,
            Autor autor,
            OffsetDateTime publicadoEm,
            String texto,
            long totalCurtidas,
            boolean curtidoPorMim,
            long totalComentarios) {}

    public record Autor(UUID id, String nome, String avatarUrl) {}
}
