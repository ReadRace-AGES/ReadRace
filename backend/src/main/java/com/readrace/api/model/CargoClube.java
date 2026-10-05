package com.readrace.api.model;

import jakarta.persistence.EnumeratedValue;

public enum CargoClube {
    ADMINISTRADOR("administrador"),
    MEMBRO("membro");

    @EnumeratedValue private final String valor;

    CargoClube(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }
}
