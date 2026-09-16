package com.readrace.api.dto.response;

public record ProgressoLeituraResponse(
        Integer paginaAtual,
        Integer paginaMaximaAlcancada,
        Integer totalPaginas,
        Integer percentual,
        Integer xpPaginas,
        Integer xpConclusao,
        Integer xpTotal,
        Boolean concluido) {}
