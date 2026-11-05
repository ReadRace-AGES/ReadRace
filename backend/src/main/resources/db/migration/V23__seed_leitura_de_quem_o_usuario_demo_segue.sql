-- V23 - Leitura de quem o usuário de demonstração segue
--
-- Reservada para a #197. A aba Social mostra a leitura das pessoas que eu sigo, mas até aqui todo
-- registro de leitura do seed era do usuário de demonstração: quem ele segue não tinha nada para
-- mostrar. Esta migration cria, para três delas (Mariana, Gabriel e Beatriz), itens de biblioteca e
-- registros de leitura nos últimos dias, com datas relativas a now().
--
-- O que a aba Social mostra com este seed:
--   Beatriz - 1984 (328 páginas): lido até a última página hoje, o "Terminou o livro" da #204.
--             Ontem voltou da 300 para a 280: a volta não gera página nem card.
--   Mariana - Orgulho e Preconceito (432 páginas): dois registros hoje (135 e 170), um único card
--             com as 60 páginas novas sobre a 110 de quatro dias atrás.
--   Gabriel - Frankenstein (280 páginas): leitura espalhada nas últimas semanas.
--
-- "Hoje" é o dia de São Paulo, como no resto do app: os registros de hoje nunca caem antes da
-- meia-noite de lá, mesmo quando a migration roda logo depois dela.
--
-- A #220 (dados de demonstração) também cria itens de biblioteca para outros usuários. O item é
-- criado só se ainda não existir e os registros se ligam ao item pelo par usuário e livro, não pelo
-- id daqui.

INSERT INTO item_biblioteca (
    id,
    usuario_id,
    livro_id,
    status_leitura,
    pagina_atual,
    pagina_maxima,
    adicionado_em
) VALUES
('42000000-0000-0000-0000-000000000001',
 '00000000-0000-0000-0000-000000000006',
 '30000000-0000-0000-0000-000000000002',
 'lendo', 0, 0, now() - INTERVAL '9 days'),

('42000000-0000-0000-0000-000000000002',
 '00000000-0000-0000-0000-000000000004',
 '30000000-0000-0000-0000-000000000003',
 'lendo', 0, 0, now() - INTERVAL '12 days'),

('42000000-0000-0000-0000-000000000003',
 '00000000-0000-0000-0000-000000000005',
 '30000000-0000-0000-0000-000000000005',
 'lendo', 0, 0, now() - INTERVAL '22 days')
ON CONFLICT (usuario_id, livro_id) DO NOTHING;

WITH hoje AS (
    SELECT date_trunc('day', now() AT TIME ZONE 'America/Sao_Paulo')
               AT TIME ZONE 'America/Sao_Paulo' AS inicio
)
INSERT INTO registro_leitura (
    id,
    item_biblioteca_id,
    ultima_pagina,
    registrado_em
)
SELECT seed.id, item.id, seed.pagina, seed.registrado_em
FROM (VALUES
    -- Beatriz - 1984
    ('43000000-0000-0000-0000-000000000001'::uuid, '00000000-0000-0000-0000-000000000006'::uuid,
     '30000000-0000-0000-0000-000000000002'::uuid, 120, now() - INTERVAL '6 days'),
    ('43000000-0000-0000-0000-000000000002'::uuid, '00000000-0000-0000-0000-000000000006'::uuid,
     '30000000-0000-0000-0000-000000000002'::uuid, 250, now() - INTERVAL '3 days'),
    ('43000000-0000-0000-0000-000000000003'::uuid, '00000000-0000-0000-0000-000000000006'::uuid,
     '30000000-0000-0000-0000-000000000002'::uuid, 300, now() - INTERVAL '1 day 2 hours'),
    ('43000000-0000-0000-0000-000000000004'::uuid, '00000000-0000-0000-0000-000000000006'::uuid,
     '30000000-0000-0000-0000-000000000002'::uuid, 280, now() - INTERVAL '1 day'),
    ('43000000-0000-0000-0000-000000000005'::uuid, '00000000-0000-0000-0000-000000000006'::uuid,
     '30000000-0000-0000-0000-000000000002'::uuid, 328,
     GREATEST(now() - INTERVAL '1 hour', (SELECT inicio FROM hoje))),

    -- Mariana - Orgulho e Preconceito
    ('43000000-0000-0000-0000-000000000006'::uuid, '00000000-0000-0000-0000-000000000004'::uuid,
     '30000000-0000-0000-0000-000000000003'::uuid, 60, now() - INTERVAL '10 days'),
    ('43000000-0000-0000-0000-000000000007'::uuid, '00000000-0000-0000-0000-000000000004'::uuid,
     '30000000-0000-0000-0000-000000000003'::uuid, 110, now() - INTERVAL '4 days'),
    ('43000000-0000-0000-0000-000000000008'::uuid, '00000000-0000-0000-0000-000000000004'::uuid,
     '30000000-0000-0000-0000-000000000003'::uuid, 135,
     GREATEST(now() - INTERVAL '3 hours', (SELECT inicio FROM hoje))),
    ('43000000-0000-0000-0000-000000000009'::uuid, '00000000-0000-0000-0000-000000000004'::uuid,
     '30000000-0000-0000-0000-000000000003'::uuid, 170,
     GREATEST(now() - INTERVAL '2 hours', (SELECT inicio FROM hoje))),

    -- Gabriel - Frankenstein
    ('43000000-0000-0000-0000-000000000010'::uuid, '00000000-0000-0000-0000-000000000005'::uuid,
     '30000000-0000-0000-0000-000000000005'::uuid, 30, now() - INTERVAL '20 days'),
    ('43000000-0000-0000-0000-000000000011'::uuid, '00000000-0000-0000-0000-000000000005'::uuid,
     '30000000-0000-0000-0000-000000000005'::uuid, 80, now() - INTERVAL '15 days'),
    ('43000000-0000-0000-0000-000000000012'::uuid, '00000000-0000-0000-0000-000000000005'::uuid,
     '30000000-0000-0000-0000-000000000005'::uuid, 125, now() - INTERVAL '2 days')
) AS seed(id, usuario_id, livro_id, pagina, registrado_em)
JOIN item_biblioteca item
  ON item.usuario_id = seed.usuario_id
 AND item.livro_id = seed.livro_id;

-- O item fica coerente com o último registro: a página atual é a do último registro e a máxima
-- nunca diminui (trigger da V3).
UPDATE item_biblioteca AS item
SET status_leitura = CAST(seed.status AS status_leitura),
    pagina_atual = seed.pagina_atual,
    pagina_maxima = GREATEST(item.pagina_maxima, seed.pagina_maxima)
FROM (VALUES
    ('00000000-0000-0000-0000-000000000006'::uuid, '30000000-0000-0000-0000-000000000002'::uuid,
     'lido', 328, 328),
    ('00000000-0000-0000-0000-000000000004'::uuid, '30000000-0000-0000-0000-000000000003'::uuid,
     'lendo', 170, 170),
    ('00000000-0000-0000-0000-000000000005'::uuid, '30000000-0000-0000-0000-000000000005'::uuid,
     'lendo', 125, 125)
) AS seed(usuario_id, livro_id, status, pagina_atual, pagina_maxima)
WHERE item.usuario_id = seed.usuario_id
  AND item.livro_id = seed.livro_id;
