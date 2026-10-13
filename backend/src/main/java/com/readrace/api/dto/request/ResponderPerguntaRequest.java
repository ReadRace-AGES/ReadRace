package com.readrace.api.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ResponderPerguntaRequest(
        @NotNull(message = "informe a alternativa") UUID alternativaId) {}
