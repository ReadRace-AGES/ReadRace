package com.readrace.api.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.RegistroLeitura;

public interface RegistroLeituraRepository extends JpaRepository<RegistroLeitura, UUID> {
    interface MedicaoSistema {
        long getMensal();

        long getSemanal();
    }

    // A primeira conclusão e as máximas são calculadas antes de filtrar os períodos.
    // Não usa status/pagina_maxima: históricos ausentes não têm data presumida.
    @Query(
            value =
                    """
            WITH por_livro AS (
                SELECT i.livro_id,
                       MIN(r.registrado_em) FILTER (
                           WHERE r.ultima_pagina >= l.total_paginas) AS primeira_conclusao,
                       COALESCE(MAX(r.ultima_pagina) FILTER (
                           WHERE r.registrado_em < :inicioSemana), 0) AS maxima_anterior,
                       COALESCE(MAX(r.ultima_pagina) FILTER (
                           WHERE r.registrado_em < :fimSemana), 0) AS maxima_ate_limite
                FROM registro_leitura r
                JOIN item_biblioteca i ON i.id = r.item_biblioteca_id
                JOIN livro l ON l.id = i.livro_id
                WHERE i.usuario_id = :usuarioId AND r.registrado_em <= :consultadoEm
                GROUP BY i.livro_id
            )
            SELECT COUNT(*) FILTER (
                       WHERE primeira_conclusao >= :inicioMes
                         AND primeira_conclusao < :fimMes) AS mensal,
                   COALESCE(SUM(GREATEST(0, maxima_ate_limite - maxima_anterior)), 0)::bigint
                       AS semanal
            FROM por_livro
            """,
            nativeQuery = true)
    MedicaoSistema medirSistema(
            @Param("usuarioId") UUID usuarioId,
            @Param("inicioMes") Instant inicioMes,
            @Param("fimMes") Instant fimMes,
            @Param("inicioSemana") Instant inicioSemana,
            @Param("fimSemana") Instant fimSemana,
            @Param("consultadoEm") Instant consultadoEm);
}
