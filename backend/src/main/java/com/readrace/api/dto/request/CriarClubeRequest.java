package com.readrace.api.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CriarClubeRequest(
        String nome,
        String descricao,
        @NotNull(message = "informe o livro") UUID livroId,
        List<@NotNull(message = "informe um usuário válido") UUID> membros) {}
