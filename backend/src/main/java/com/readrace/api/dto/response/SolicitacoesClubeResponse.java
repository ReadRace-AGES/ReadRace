package com.readrace.api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SolicitacoesClubeResponse(List<Solicitacao> solicitacoes) {
    public record Solicitacao(UUID id, Usuario usuario, OffsetDateTime criadaEm) {}

    public record Usuario(UUID id, String nome, String nomeUsuario, String avatarUrl) {}
}
