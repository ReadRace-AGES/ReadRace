package com.readrace.api.dto.response;

import java.util.List;
import java.util.UUID;

public record RankingClubeResponse(
        Clube clube, List<ClubeResponse.LinhaRanking> ranking, String proximoCursor) {

    public record Clube(UUID id, String nome) {}
}
