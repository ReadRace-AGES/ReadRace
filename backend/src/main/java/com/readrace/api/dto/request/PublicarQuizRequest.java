package com.readrace.api.dto.request;

import java.util.List;

/**
 * Sem Bean Validation de propósito: toda falha do corpo responde {@code QUIZ_INVALIDO}, o código do
 * contrato, e não o {@code VALIDATION_ERROR} genérico. A validação fica em {@code
 * PublicarQuizService}.
 */
public record PublicarQuizRequest(
        String titulo, Integer recompensaXp, List<PerguntaRequest> perguntas) {

    /** {@code alternativas} traz os 4 textos na ordem A..D; {@code correta} é o índice 0..3. */
    public record PerguntaRequest(String enunciado, List<String> alternativas, Integer correta) {}
}
