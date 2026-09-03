package com.readrace.api.dto.response;

import java.util.UUID;

import com.readrace.api.model.UsuarioId;

public record UsuarioAtualResponse(UUID id) {
    public static UsuarioAtualResponse de(UsuarioId id) {
        return new UsuarioAtualResponse(id.valor());
    }
}
