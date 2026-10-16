package com.readrace.api.dto.response;

import java.util.UUID;

import com.readrace.api.model.Mascote;

public record MascoteResponse(UUID id, String nome, String imagem) {

    public static MascoteResponse de(Mascote mascote) {
        return new MascoteResponse(mascote.getId(), mascote.getNome(), mascote.getImagem());
    }
}
