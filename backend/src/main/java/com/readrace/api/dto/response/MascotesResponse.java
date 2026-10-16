package com.readrace.api.dto.response;

import java.util.List;
import java.util.UUID;

import com.readrace.api.model.Mascote;

public record MascotesResponse(List<Item> mascotes) {

    public record Item(
            UUID id,
            String nome,
            String imagem,
            int nivelMinimo,
            boolean desbloqueado,
            boolean escolhido) {

        public static Item de(Mascote mascote, int nivelDoUsuario, UUID escolhido) {
            return new Item(
                    mascote.getId(),
                    mascote.getNome(),
                    mascote.getImagem(),
                    mascote.getNivelMinimo(),
                    mascote.desbloqueadoNoNivel(nivelDoUsuario),
                    mascote.getId().equals(escolhido));
        }
    }
}
