package com.readrace.api.model;

/**
 * Estado de leitura de um item da biblioteca.
 *
 * <p>As constantes ficam em minusculas de proposito: o Hibernate grava e le o {@code name()} no tipo
 * {@code status_leitura} do Postgres (V2), cujos literais sao {@code lendo}, {@code lido} e {@code
 * desejo}. Favorito nao e estado: e a flag {@link ItemBiblioteca#isFavorito()}.
 */
public enum StatusLeitura {
    lendo,
    lido,
    desejo
}
