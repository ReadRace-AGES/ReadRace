package com.readrace.api.dto.request;

/** A identidade e o cargo do criador vêm exclusivamente da sessão autenticada. */
public record CriarComunidadeRequest(String nome, String descricao, String imagemUrl) {}
