package com.readrace.api.dto.response;

import java.util.UUID;

import com.readrace.api.model.Conquista;

public record ConquistaDesbloqueadaResponse(UUID id, String nome, Integer xp) {

    public static ConquistaDesbloqueadaResponse de(Conquista conquista) {
        return new ConquistaDesbloqueadaResponse(
                conquista.getId(), conquista.getNome(), conquista.getRecompensaXp());
    }
}
