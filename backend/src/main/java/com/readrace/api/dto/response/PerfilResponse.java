package com.readrace.api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

public record PerfilResponse(
        UUID id,
        String nome,
        String username,
        String email,
        String avatar,
        String titulo,
        int nivel,
        int xpAtual,
        int xpNoNivel,
        int xpDoNivel,
        long seguidores,
        long seguindo,
        Estatisticas estatisticas,
        List<Conquista> conquistas,
        List<Favorito> livrosFavoritos,
        // Só o perfil próprio traz o mascote; no de outro usuário o campo nem aparece.
        @JsonInclude(JsonInclude.Include.NON_NULL) Mascote mascote) {
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

    public record Mascote(String nome, String imagem) {

        public static Mascote de(com.readrace.api.model.Mascote mascote) {
            return new Mascote(mascote.getNome(), mascote.getImagem());
        }
    }
}
