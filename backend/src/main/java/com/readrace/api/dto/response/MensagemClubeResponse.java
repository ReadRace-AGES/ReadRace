package com.readrace.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MensagemClubeResponse(
        UUID id,
        String texto,
        OffsetDateTime enviadaEm,
        boolean minha,
        Autor autor
) {

    public record Autor(
            UUID id,
            String nome,
            String avatarUrl
    ) {
    }
}