UPDATE desafio_amigo AS desafio
SET titulo = novo.titulo,
    descricao = novo.descricao
FROM (VALUES
    ('90000000-0000-0000-0000-000000000001'::uuid, 'Desafio de 300 páginas', 'Quem lê mais páginas em 14 dias'),
    ('90000000-0000-0000-0000-000000000002'::uuid, 'Desafio de 300 páginas', 'Quem lê mais páginas em 14 dias'),
    ('90000000-0000-0000-0000-000000000003'::uuid, 'Desafio de leitura', 'Quem termina A Metamorfose primeiro'),
    ('90000000-0000-0000-0000-000000000004'::uuid, 'Desafio de 150 páginas', 'Quem lê mais páginas em 3 dias')
) AS novo (id, titulo, descricao)
WHERE desafio.id = novo.id;

UPDATE desafio_amigo
SET status = 'ativo',
    inicio_em = now(),
    fim_em = now() + INTERVAL '3 days'
WHERE id = '90000000-0000-0000-0000-000000000004'
  AND status = 'pendente';
