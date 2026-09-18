package com.readrace.api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PerfilResponse(
        UUID id,
        String nome,
        String username,
        String avatar,
        String titulo,
        int nivel,
        int xpAtual,
        long seguidores,
        long seguindo,
        Estatisticas estatisticas,
        List<Conquista> conquistas,
        List<Favorito> livrosFavoritos) {
    public record Estatisticas(
            long livrosLidos, long paginasLidas, int sequenciaDias, long conquistas) {}

    public record Conquista(
            UUID id,
            String nome,
            String icone,
            String descricao,
            boolean desbloqueada,
            OffsetDateTime data) {}

    public record Favorito(UUID id, String titulo, String autor, String capa) {}
}
