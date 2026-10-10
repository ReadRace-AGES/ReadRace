package com.readrace.api.model;

import java.util.Arrays;
import java.util.List;

public enum CriterioConquista {
    LIVROS_LIDOS("livros_lidos"),
    PAGINAS_LIDAS("paginas_lidas"),
    DESAFIOS("desafios"),
    CLUBES("clubes");

    private final String valor;

    CriterioConquista(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    public static List<String> valores() {
        return Arrays.stream(values()).map(CriterioConquista::getValor).toList();
    }

    public static CriterioConquista de(String valor) {
        return Arrays.stream(values())
                .filter(criterio -> criterio.valor.equals(valor))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Critério sem avaliação: " + valor));
    }
}
