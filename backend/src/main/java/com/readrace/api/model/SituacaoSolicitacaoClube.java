package com.readrace.api.model;

import jakarta.persistence.EnumeratedValue;

public enum SituacaoSolicitacaoClube {
    PENDENTE("pendente"),
    ACEITA("aceita"),
    RECUSADA("recusada");

    @EnumeratedValue private final String valor;

    SituacaoSolicitacaoClube(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }
}
