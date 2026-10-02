package com.readrace.api.dto.response;

/** Estado da curtida do usuário atual em um post, depois de curtir ou descurtir (#101). */
public record CurtidaResponse(boolean curtidoPorMim, long totalCurtidas) {}
