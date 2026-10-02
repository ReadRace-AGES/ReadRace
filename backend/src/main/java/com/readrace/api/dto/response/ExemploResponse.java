package com.readrace.api.dto.response;

import com.readrace.api.model.Exemplo;

/** O que a API devolve. Converta a entidade aqui — nunca serialize a entidade direto. */
public record ExemploResponse(Long id, String nome, String descricao) {

    public static ExemploResponse de(Exemplo exemplo) {
        return new ExemploResponse(exemplo.getId(), exemplo.getNome(), exemplo.getDescricao());
    }
}
