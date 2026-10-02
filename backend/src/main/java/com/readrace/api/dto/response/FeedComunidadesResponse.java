package com.readrace.api.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Resposta de {@code GET /api/feed/comunidades} (#34): os clubes do livro e as comunidades de que o
 * usuário atual é membro, como duas listas separadas. A distinção entre {@code ClubeDoLivro} e
 * {@code Comunidade} é estrutural (produto §3): só o clube tem {@code livroAtual}.
 */
public record FeedComunidadesResponse(
        Usuario usuario, List<Clube> clubes, List<Comunidade> comunidades) {

    public record Usuario(String nome, Integer sequenciaDias) {}

    public record Clube(
            UUID id, String nome, String capaUrl, LivroAtual livroAtual, long totalMembros) {}

    public record LivroAtual(String titulo, String autor) {}

    public record Comunidade(UUID id, String nome, String capaUrl, long totalMembros) {}
}
