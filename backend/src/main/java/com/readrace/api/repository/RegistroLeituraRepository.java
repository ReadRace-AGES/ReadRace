package com.readrace.api.repository;

import java.time.Instant;
import java.util.List;
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

    /**
     * Um card da aba Social: o que uma pessoa leu de um livro em um dia ({@code leitura}, #197) ou
     * o livro que ela terminou ({@code conclusao}, #204).
     */
    interface AtividadeSocial {
        String getTipo();

        UUID getUsuarioId();

        UUID getLivroId();

        int getPaginas();

        Instant getEm();
    }

    // As páginas novas de um dia partem da maior página alcançada em qualquer registro anterior
    // daquele livro, não só do próprio dia: voltar a página e relê-la não conta de novo. Por isso
    // a máxima é calculada sobre todo o histórico e só depois o período é filtrado.
    //
    // A conclusão é o registro que chegou à última página pela primeira vez: o que veio antes dele
    // não tinha alcançado o fim. Voltar a página e chegar ao fim de novo não conclui outra vez.
    // Ela entra além da leitura daquele dia e, no empate de horário, vem antes dela.
    @Query(
            value =
                    """
            WITH registros AS (
                SELECT i.usuario_id, i.livro_id, l.total_paginas, r.ultima_pagina,
                       r.registrado_em,
                       CAST(r.registrado_em AT TIME ZONE :fuso AS date) AS dia,
                       COALESCE(MAX(r.ultima_pagina) OVER (
                           PARTITION BY i.id ORDER BY r.registrado_em, r.id
                           ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0)
                           AS maxima_anterior
                FROM seguir s
                JOIN usuario u ON u.id = s.seguido_id
                JOIN item_biblioteca i ON i.usuario_id = s.seguido_id
                JOIN registro_leitura r ON r.item_biblioteca_id = i.id
                JOIN livro l ON l.id = i.livro_id
                WHERE s.seguidor_id = :usuarioId
                  AND u.excluido_em IS NULL
                  AND r.registrado_em <= :agora
            ),
            leituras AS (
                SELECT 'leitura' AS tipo, usuario_id, livro_id,
                       MAX(ultima_pagina) - MIN(maxima_anterior) AS paginas,
                       MAX(registrado_em) AS em
                FROM registros
                GROUP BY usuario_id, livro_id, dia
                HAVING MAX(ultima_pagina) > MIN(maxima_anterior)
            ),
            conclusoes AS (
                SELECT 'conclusao' AS tipo, usuario_id, livro_id, total_paginas AS paginas,
                       registrado_em AS em
                FROM registros
                WHERE ultima_pagina >= total_paginas
                  AND maxima_anterior < total_paginas
            ),
            atividades AS (
                SELECT * FROM leituras
                UNION ALL
                SELECT * FROM conclusoes
            )
            SELECT tipo, usuario_id AS usuarioId, livro_id AS livroId, paginas, em
            FROM atividades
            WHERE em >= :desde
            ORDER BY em DESC, tipo = 'conclusao' DESC, usuario_id, livro_id
            LIMIT :limite
            """,
            nativeQuery = true)
    List<AtividadeSocial> buscarAtividadesSociais(
            @Param("usuarioId") UUID usuarioId,
            @Param("fuso") String fuso,
            @Param("desde") Instant desde,
            @Param("agora") Instant agora,
            @Param("limite") int limite);
}
