package com.readrace.api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resposta de {@code GET /api/feed/social} (#197, #204): a leitura e os livros terminados das
 * pessoas que o usuário atual segue, da mais recente para a mais antiga.
 *
 * <p>A atividade não é um post gravado (decisão do gestor em 15/10): a API monta a lista na hora a
 * partir do registro de leitura, por isso não há curtida nem comentário. {@code em} é o horário do
 * último registro daquele dia, no fuso de São Paulo.
 */
public record FeedSocialResponse(List<Atividade> atividades) {

    /**
     * {@code tipo} é {@code leitura} (as páginas novas do dia) ou {@code conclusao} (o livro
     * terminado, com {@code paginas} igual ao total do livro e o bônus de conclusão em {@code xp}).
     */
    public record Atividade(
            String tipo, Usuario usuario, Livro livro, int paginas, int xp, OffsetDateTime em) {}

    /** {@code sequenciaDias} alimenta o badge de chama, como nos posts. */
    public record Usuario(UUID id, String nome, String avatarUrl, Integer sequenciaDias) {}

    public record Livro(UUID id, String titulo, String autor, int totalPaginas) {}
}
