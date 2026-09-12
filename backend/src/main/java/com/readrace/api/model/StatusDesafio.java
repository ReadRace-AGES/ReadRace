package com.readrace.api.model;

import jakarta.persistence.EnumeratedValue;

public enum StatusDesafio {
    PENDENTE("pendente"),
    ATIVO("ativo"),
    RECUSADO("recusado"),
    FINALIZADO("finalizado");

    @EnumeratedValue private final String valor;

    StatusDesafio(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }
}
