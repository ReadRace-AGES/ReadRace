package com.readrace.api.dto.response;

import java.util.UUID;

import com.readrace.api.model.Comunidade;

public record ComunidadeBuscaResponse(UUID id, String nome, String descricao, String capa) {

    public static ComunidadeBuscaResponse de(Comunidade comunidade) {
        return new ComunidadeBuscaResponse(
                comunidade.getId(),
                comunidade.getNome(),
                comunidade.getDescricao(),
                comunidade.getImagemUrl());
    }
}
