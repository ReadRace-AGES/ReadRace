package com.readrace.api.dto.response;

import java.util.List;

public record MensagensClubeResponse(
        List<MensagemClubeResponse> mensagens
) {
}