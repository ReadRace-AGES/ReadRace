package com.readrace.api.dto.response;

import java.util.UUID;

public record ComunidadeCriadaResponse(
        UUID id, String nome, String descricao, String imagemUrl, long seguidores, boolean sigo) {}
