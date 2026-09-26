WITH RECURSIVE curva (nivel, acumulado) AS (
    SELECT 1, 0::bigint
    UNION ALL
    SELECT nivel + 1, acumulado + floor(45 * nivel * sqrt(nivel))::bigint
    FROM curva
    WHERE nivel < 2000
)
UPDATE usuario u
SET nivel = (SELECT max(c.nivel) FROM curva c WHERE c.acumulado <= u.xp_total);