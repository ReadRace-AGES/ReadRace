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
 *
 * <p>{@code meuCargo} é {@code "administrador"} ou {@code "membro"} quando o usuário atual
 * participa do clube, e {@code null} quando não participa (#160).
 *
 * <p>{@code temQuizAtivo} diz se o clube já tem um quiz publicado. O administrador não pode abrir
 * {@code GET /quiz} (responde 403), e é por aqui que a configuração sabe se vai substituir um quiz
 * (#161).
 */
public record ClubeResponse(
        UUID id,
        String nome,
        LivroAtual livroAtual,
        List<LinhaRanking> ranking,
        String meuCargo,
        boolean temQuizAtivo) {

    /** {@code id} vem porque "Registrar leitura" precisa saber sobre qual livro o modal abre. */
    public record LivroAtual(UUID id, String titulo, String autor) {}

    /** {@code posicao} começa em 1 e é calculada aqui: a tela não ordena nem numera. */
    public record LinhaRanking(int posicao, Usuario usuario, int pontos) {}

    public record Usuario(UUID id, String nome, String avatarUrl) {}
}
