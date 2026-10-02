package com.readrace.api.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import com.readrace.api.model.TipoMetaDesafio;

public record CriarDesafioRequest(
        @NotNull(message = "informe o oponente") UUID oponenteId,
        @NotNull(message = "informe o tipo de meta") TipoMetaDesafio tipoMeta,
        Integer meta,
        UUID livroId,
        Integer prazoDias) {}
