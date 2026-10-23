package com.readrace.api.dto.request;

/** {@code lista} é "lido", "desejo" ou "favorito" (#155). */
public record AdicionarLivroRequest(String volumeId, String lista) {}
