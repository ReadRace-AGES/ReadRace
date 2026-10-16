package com.readrace.api.dto.response;

import java.util.UUID;

public record QuizPublicadoResponse(UUID id, String titulo, int totalPerguntas) {}
