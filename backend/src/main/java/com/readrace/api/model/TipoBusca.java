package com.readrace.api.model;

import com.readrace.api.exception.ParametroInvalidoException;

public enum TipoBusca {
    LIVROS("livros"),
    USUARIOS("usuarios"),
    COMUNIDADES("comunidades");

    private final String valor;

    TipoBusca(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static TipoBusca de(String valor) {
        for (TipoBusca tipo : values()) {
            if (tipo.valor.equals(valor)) {
                return tipo;
            }
        }

        throw new ParametroInvalidoException("O parâmetro 'tipo' deve ser livros, usuarios ou comunidades.");
    }
}