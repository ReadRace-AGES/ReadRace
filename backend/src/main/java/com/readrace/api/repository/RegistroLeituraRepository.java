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

    // GROUPS exclui todos os empates da máxima anterior; o incremento legado é contado
    // uma vez por instante. Intervalos explícitos sobrepostos são descontados do legado.
    // Todo o histórico é calculado antes de filtrar os períodos, sem presumir datas por status.
    @Query(
            value =
                    """
            WITH historico AS (
                SELECT i.livro_id, r.registrado_em, r.ultima_pagina, r.paginas_novas,
                       l.total_paginas,
                       COALESCE(MAX(r.ultima_pagina) OVER (
                           PARTITION BY i.livro_id ORDER BY r.registrado_em
                           GROUPS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0)
                           AS maxima_anterior,
                       MAX(r.ultima_pagina) FILTER (WHERE r.paginas_novas IS NULL) OVER (
                           PARTITION BY i.livro_id, r.registrado_em) AS maxima_legada
                FROM registro_leitura r
                JOIN item_biblioteca i ON i.id = r.item_biblioteca_id
                JOIN livro l ON l.id = i.livro_id
                WHERE i.usuario_id = :usuarioId AND r.registrado_em <= :consultadoEm
            ), por_instante AS (
                SELECT livro_id, registrado_em,
                       COALESCE(SUM(paginas_novas), 0) + GREATEST(0,
                           COALESCE(MAX(maxima_legada), 0) - MAX(maxima_anterior)
                           - COALESCE(SUM(GREATEST(0,
                               LEAST(ultima_pagina, maxima_legada)
                               - GREATEST(ultima_pagina - paginas_novas, maxima_anterior)))
                               FILTER (WHERE paginas_novas > 0 AND maxima_legada IS NOT NULL), 0))
                           AS paginas,
                       BOOL_OR(ultima_pagina >= total_paginas AND (
                           paginas_novas > 0 OR (paginas_novas IS NULL
                               AND ultima_pagina > maxima_anterior))) AS conclusao_elegivel
                FROM historico
                GROUP BY livro_id, registrado_em
            ), por_livro AS (
                SELECT livro_id,
                       MIN(registrado_em) FILTER (WHERE conclusao_elegivel) AS primeira_conclusao,
                       COALESCE(SUM(paginas) FILTER (
                           WHERE registrado_em >= :inicioSemana
                             AND registrado_em < :fimSemana), 0) AS semanal
                FROM por_instante
                GROUP BY livro_id
            )
            SELECT COUNT(*) FILTER (
                       WHERE primeira_conclusao >= :inicioMes
                         AND primeira_conclusao < :fimMes) AS mensal,
                   COALESCE(SUM(semanal), 0)::bigint AS semanal
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
