package com.readrace.api.dto.response;

/**
 * Resposta de {@code POST /api/clubes/{clubeId}/quiz/perguntas/{perguntaId}/resposta} (#162).
 *
 * <p>{@code pontosGanhos} são {@code Pontos} do clube; {@code resumo.xp} é {@code XP} do usuário.
 * As duas moedas nunca se convertem. {@code resumo} só vem quando esta resposta concluiu o quiz.
 */
public record RespostaQuizResponse(
        boolean acertou, int pontosGanhos, boolean concluiu, Resumo resumo) {

    public record Resumo(int acertos, int total, int pontos, int xp) {}
}
