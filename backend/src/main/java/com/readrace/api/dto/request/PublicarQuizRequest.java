package com.readrace.api.dto.request;

import java.util.List;

public record PublicarQuizRequest(
        String titulo, Integer recompensaXp, List<PerguntaRequest> perguntas) {

    /** {@code alternativas} traz os 4 textos na ordem A..D; {@code correta} é o índice 0..3. */
    public record PerguntaRequest(String enunciado, List<String> alternativas, Integer correta) {}
}