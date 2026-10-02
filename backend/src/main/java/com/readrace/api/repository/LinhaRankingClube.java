package com.readrace.api.repository;

import java.util.UUID;

/**
 * Projeção de uma linha do ranking de {@code Pontos} do clube: o membro e quantos pontos ele tem.
 *
 * <p>{@code pontos} é {@code membro_clube.pontos} — a moeda do clube. {@code usuario.xp_total} é
 * outra coisa e não entra aqui (produto §5).
 */
public interface LinhaRankingClube {
    UUID getUsuarioId();

    String getNome();

    String getAvatarUrl();

    int getPontos();
}
