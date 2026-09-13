package com.readrace.api.dto.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

public record DesafioResponse(
        UUID id,
        OponenteDesafioResponse oponente,
        String descricao,
        String tipoMeta,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer meta,
        LivroDesafioResponse livro,
        Integer prazoDias,
        Integer diasRestantes,
        String status,
        PlacarDesafioResponse progresso) {}
