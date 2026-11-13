package com.readrace.api.dto.response;

import java.util.UUID;

public record SairDoClubeResponse(NovoLider novoLider) {
    public record NovoLider(UUID id, String nome) {}
}
