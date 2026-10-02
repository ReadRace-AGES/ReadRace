package com.readrace.api.model;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoMetaDesafio {
    PAGINAS("paginas"),
    LIVRO("livro");

    @EnumeratedValue private final String valor;

    TipoMetaDesafio(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static TipoMetaDesafio de(String valor) {
        for (TipoMetaDesafio tipo : values()) {
            if (tipo.valor.equals(valor)) {
                return tipo;
            }
        }

        throw new IllegalArgumentException("Tipo de meta inválido.");
    }
}
