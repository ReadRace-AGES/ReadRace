package com.readrace.api.model;

import jakarta.persistence.EnumeratedValue;

public enum Cargo {
    ADMINISTRADOR("administrador"),
    MEMBRO("membro");

    @EnumeratedValue private final String valor;

    Cargo(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }
}
