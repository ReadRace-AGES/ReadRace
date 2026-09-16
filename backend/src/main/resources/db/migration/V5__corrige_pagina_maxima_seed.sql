-- Corrige apenas os itens que ainda possuem os valores inconsistentes da V4.
-- Preserva avanços registrados depois do seed e não altera itens de outros usuários.
-- A suspensão do trigger fica restrita a esta migration transacional do Flyway.
ALTER TABLE item_biblioteca DISABLE TRIGGER validar_pagina_maxima_antes_de_atualizar;

UPDATE item_biblioteca AS item
SET pagina_maxima = GREATEST(
    item.pagina_atual,
    COALESCE((SELECT MAX(registro.ultima_pagina)
              FROM registro_leitura AS registro
              WHERE registro.item_biblioteca_id = item.id), 0)
)
FROM (VALUES
    ('40000000-0000-0000-0000-000000000001'::uuid, 'lendo', 145, 256),
    ('40000000-0000-0000-0000-000000000002'::uuid, 'lendo', 210, 328),
    ('40000000-0000-0000-0000-000000000005'::uuid, 'desejo', 0, 280),
    ('40000000-0000-0000-0000-000000000006'::uuid, 'lendo', 310, 720),
    ('40000000-0000-0000-0000-000000000008'::uuid, 'desejo', 0, 300),
    ('40000000-0000-0000-0000-000000000010'::uuid, 'lendo', 94, 288),
    ('40000000-0000-0000-0000-000000000011'::uuid, 'desejo', 0, 304)
) AS seed(id, status, pagina_atual, pagina_maxima)
WHERE item.id = seed.id
  AND item.usuario_id = '00000000-0000-0000-0000-000000000001'::uuid
  AND item.status_leitura::text = seed.status
  AND item.pagina_atual = seed.pagina_atual
  AND item.pagina_maxima = seed.pagina_maxima;

ALTER TABLE item_biblioteca ENABLE TRIGGER validar_pagina_maxima_antes_de_atualizar;
