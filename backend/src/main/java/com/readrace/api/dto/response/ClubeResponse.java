package com.readrace.api.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Resposta de {@code GET /api/clubes/{clubeId}} (#35): o cabeçalho da Página do clube e o ranking
 * de membros.
 *
 * <p>É um endpoint de {@code ClubeDoLivro}, e só dele: {@code Comunidade} não tem livro atual nem
 * ranking (produto §3). {@code livroAtual} nunca vem vazio, porque {@code clube_do_livro.livro_id}
 * é {@code NOT NULL}.
 *
 * <p>A moeda do ranking é {@code Pontos} ({@code membro_clube.pontos}), do clube. Nenhum campo de
 * {@code XP} entra nesta resposta (produto §5).
 */
public record ClubeResponse(
        UUID id, String nome, LivroAtual livroAtual, List<LinhaRanking> ranking) {

    /** {@code id} vem porque "Registrar leitura" precisa saber sobre qual livro o modal abre. */
    public record LivroAtual(UUID id, String titulo, String autor) {}

    /** {@code posicao} começa em 1 e é calculada aqui: a tela não ordena nem numera. */
    public record LinhaRanking(int posicao, Usuario usuario, int pontos) {}

    public record Usuario(UUID id, String nome, String avatarUrl) {}
}
