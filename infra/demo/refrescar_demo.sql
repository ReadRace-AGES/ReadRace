-- Refrescar a demo (#220)
--
-- O seed foi escrito relativo à data em que cada migration rodou, então envelhece: a sequência
-- zera, o desafio vence e os posts aparecem como "há 3 semanas". Este script reaplica as datas
-- do seed com o now() de agora, como se o seed tivesse acabado de ser criado.
--
-- Regras:
-- - Só mexe em linhas do seed, encontradas pelos ids fixos e pelos md5 das migrations. Nunca
--   pelo autor: a conta demo entra como o Daniel, e o que ela faz no app tem id aleatório.
-- - Pode rodar mais de uma vez: cada rodada calcula as datas a partir do now() dela.
-- - Rodar numa transação só (psql -1). Como usar: infra/README.md, "Refrescar a demo".

-- 1. Sequência do Daniel: última leitura ontem (dia de São Paulo, o mesmo do app) e 12 dias,
--    o valor da V4. A chama fica apagada e acende quando ele registra uma leitura na demo.
UPDATE usuario
SET dias_consecutivos = 12,
    ultima_leitura_em = (now() AT TIME ZONE 'America/Sao_Paulo')::date - 1
WHERE id = '00000000-0000-0000-0000-000000000001';

-- 2. Leituras do seed (V4, seção 8), com os mesmos dias atrás.
UPDATE registro_leitura AS r
SET registrado_em = now() - make_interval(days => s.dias)
FROM (VALUES (1, 14), (2, 10), (3, 7), (4, 3), (5, 1),
             (6, 12), (7, 8), (8, 2),
             (9, 18), (10, 11), (11, 6), (12, 1),
             (13, 9), (14, 5), (15, 1)) AS s (n, dias)
WHERE r.id = ('41000000-0000-0000-0000-' || lpad(s.n::text, 12, '0'))::uuid;

-- 3. Posts dos clubes (V4, seção 14): p + c dias atrás.
UPDATE post
SET criado_em = s.quando, atualizado_em = s.quando
FROM (SELECT md5('post-clube-' || c || '-' || p)::uuid AS id,
             now() - make_interval(days => p + c) AS quando
      FROM generate_series(1, 5) AS c
      CROSS JOIN generate_series(1, 6) AS p) AS s
WHERE post.id = s.id;

-- 4. Posts das comunidades (V4, seção 15): p * 2 + c horas atrás.
UPDATE post
SET criado_em = s.quando, atualizado_em = s.quando
FROM (SELECT md5('post-comunidade-' || c || '-' || p)::uuid AS id,
             now() - make_interval(hours => p * 2 + c) AS quando
      FROM generate_series(1, 5) AS c
      CROSS JOIN generate_series(1, 6) AS p) AS s
WHERE post.id = s.id;

-- 5. Posts sobre livros (V4, seção 16).
UPDATE post
SET criado_em = now() - make_interval(days => s.dias),
    atualizado_em = now() - make_interval(days => s.dias)
FROM (VALUES (1, 2), (2, 1), (3, 4), (4, 3), (5, 5), (6, 6)) AS s (n, dias)
WHERE post.id = ('72000000-0000-0000-0000-' || lpad(s.n::text, 12, '0'))::uuid;

-- 6. Comentários nas comunidades (V19): 20 minutos depois do post, um após o outro.
UPDATE post
SET criado_em = s.quando, atualizado_em = s.quando
FROM (SELECT md5('comentario-comunidade-' || c || '-' || p || '-' || k)::uuid AS id,
             now() - make_interval(hours => p * 2 + c) + make_interval(mins => k * 20) AS quando
      FROM generate_series(1, 5) AS c
      CROSS JOIN generate_series(1, 3) AS p
      CROSS JOIN generate_series(1, 3) AS k
      WHERE k <= 4 - p) AS s
WHERE post.id = s.id;

-- 7. Respostas aos comentários (V19): 5 minutos depois do comentário que respondem.
UPDATE post
SET criado_em = s.quando, atualizado_em = s.quando
FROM (SELECT md5('resposta-comunidade-' || c || '-' || k)::uuid AS id,
             now() - make_interval(hours => 2 + c) + make_interval(mins => 20 + k * 5) AS quando
      FROM generate_series(1, 5) AS c
      CROSS JOIN generate_series(1, 2) AS k) AS s
WHERE post.id = s.id;

-- 8. Desafio Daniel x Ana (V4, seção 19), em andamento. O prazo de 10 dias cobre a banca de
--    18/11 rodando em 09/11 ou 16/11. Reativar é seguro: finalizar não paga XP, só reavalia
--    conquistas, que não se repetem.
UPDATE desafio_amigo
SET status = 'ativo',
    inicio_em = now() - INTERVAL '5 days',
    fim_em = now() + INTERVAL '10 days',
    criado_em = now() - INTERVAL '6 days'
WHERE id = '90000000-0000-0000-0000-000000000001';

-- Placar perto da meta de 300 páginas, com o Daniel na frente.
UPDATE progresso_desafio AS pd
SET valor_atual = s.valor,
    aceito_em = now() - INTERVAL '5 days',
    atualizado_em = now() - make_interval(hours => s.horas)
FROM (VALUES ('91000000-0000-0000-0000-000000000001'::uuid, 240, 2),
             ('91000000-0000-0000-0000-000000000002'::uuid, 210, 4)) AS s (id, valor, horas)
WHERE pd.id = s.id;
