package com.readrace.api.dto.response;

import java.util.List;
import java.util.UUID;

import com.readrace.api.model.AlternativaQuiz;
import com.readrace.api.model.PerguntaQuiz;
import com.readrace.api.model.Quiz;

/**
 * Resposta de {@code GET /api/clubes/{clubeId}/quiz} (#162): o quiz ativo do clube e a próxima
 * pergunta que o membro ainda não respondeu.
 *
 * <p>{@code proximaPergunta} é {@code null} quando o membro já respondeu todas. As alternativas não
 * dizem qual é a correta: quem decide se o membro acertou é a API, na resposta.
 */
public record QuizResponse(
        UUID id,
        String titulo,
        Livro livro,
        int recompensaXp,
        int totalPerguntas,
        Pergunta proximaPergunta) {

    public record Livro(String titulo) {}

    public record Pergunta(UUID id, int numero, String enunciado, List<Alternativa> alternativas) {}

    public record Alternativa(UUID id, String letra, String texto) {}

    public static QuizResponse de(Quiz quiz, int totalPerguntas, PerguntaQuiz proximaPergunta) {
        return new QuizResponse(
                quiz.getId(),
                quiz.getTitulo(),
                new Livro(quiz.getLivro().getTitulo()),
                quiz.getRecompensaXp(),
                totalPerguntas,
                proximaPergunta == null ? null : pergunta(proximaPergunta));
    }

    private static Pergunta pergunta(PerguntaQuiz pergunta) {
        return new Pergunta(
                pergunta.getId(),
                pergunta.getOrdem(),
                pergunta.getEnunciado(),
                pergunta.getAlternativas().stream().map(QuizResponse::alternativa).toList());
    }

    private static Alternativa alternativa(AlternativaQuiz alternativa) {
        return new Alternativa(alternativa.getId(), alternativa.getLetra(), alternativa.getTexto());
    }
}
