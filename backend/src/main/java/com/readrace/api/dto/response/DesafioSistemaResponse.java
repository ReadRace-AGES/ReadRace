package com.readrace.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.readrace.api.model.DesafioSistema;

public record DesafioSistemaResponse(
        @JsonInclude(JsonInclude.Include.ALWAYS) String titulo,
        String descricao,
        int meta,
        int progresso,
        String unidade,
        int diasRestantes,
        int xp,
        boolean cumprido) {
    public static DesafioSistemaResponse de(DesafioSistema desafio, long bruto, int diasRestantes) {
        return new DesafioSistemaResponse(
                desafio.titulo(),
                desafio.descricao(),
                desafio.meta(),
                (int) Math.min(desafio.meta(), bruto),
                desafio.unidade(),
                diasRestantes,
                desafio.xp(),
                bruto >= desafio.meta());
    }
}
