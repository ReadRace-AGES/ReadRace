package com.readrace.api.model;

/**
 * Estado de leitura de um item da biblioteca.
 *
 * <p>As constantes ficam em minúsculas de propósito: o Hibernate grava e lê o {@code name()} no
 * tipo {@code status_leitura} do Postgres (V2), cujos literais são {@code lendo}, {@code lido} e
 * {@code desejo}. Favorito não é estado: é a flag {@link ItemBiblioteca#isFavorito()}.
 */
public enum StatusLeitura {
    lendo,
    lido,
    desejo
}
