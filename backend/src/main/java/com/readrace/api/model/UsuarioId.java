package com.readrace.api.model;

import java.util.UUID;

public record UsuarioId(UUID valor) {
    public UsuarioId {
        if (valor == null) {
            throw new IllegalArgumentException("id de usuário não pode ser nulo");
        }
    }
}
