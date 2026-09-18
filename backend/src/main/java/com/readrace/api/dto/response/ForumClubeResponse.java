package com.readrace.api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resposta de {@code GET /api/clubes/{clubeId}/posts} (#36): os posts do clube sobre a leitura
 * atual, com o cabeçalho junto para a tela renderizar sem uma segunda chamada.
 *
 * <p>Vale a invariante do banco: um {@code Post} pertence a um {@code ClubeDoLivro} ou a uma {@code
 * Comunidade}, nunca aos dois. Aqui só entram posts raiz deste clube — comentário é post com pai e
 * fica de fora.
 */
public record ForumClubeResponse(Clube clube, List<Post> posts) {

    public record Clube(UUID id, String nome, LivroAtual livroAtual) {}

    public record LivroAtual(String titulo, String autor, String capaUrl) {}

    /** {@code texto} vem inteiro: o corte e o "Ler mais" são decisão do {@code PostCard} (#22). */
    public record Post(
            UUID id, Autor autor, OffsetDateTime publicadoEm, String texto, long totalCurtidas) {}

    /**
     * {@code sequenciaDias} é {@code usuario.dias_consecutivos} e alimenta o badge de chama. Não há
     * campo dizendo se eu curti, porque não há ação de curtir nesta sprint (decisão 4).
     */
    public record Autor(UUID id, String nome, String avatarUrl, Integer sequenciaDias) {}
}
