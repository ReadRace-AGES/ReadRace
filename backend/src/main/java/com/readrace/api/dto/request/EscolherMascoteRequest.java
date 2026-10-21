package com.readrace.api.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record EscolherMascoteRequest(@NotNull(message = "informe o mascote") UUID mascoteId) {}
