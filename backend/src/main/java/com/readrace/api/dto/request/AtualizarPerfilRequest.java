package com.readrace.api.dto.request;

/** Campos opcionais: o app manda só o que mudou. null = não alterar. */
public record AtualizarPerfilRequest(String nome, String username) {}
