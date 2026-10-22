-- V19 - Comentários e respostas de seed nos posts das comunidades (#150)
--
-- Comentário é uma linha em post com post_pai_id e sem clube, livro ou comunidade
-- (chk_comentario_sem_escopo): a comunidade vem do post pai.
--
-- Nos 3 posts mais recentes de cada comunidade do V4 (p = 1, 2 e 3), entram 3, 2 e 1
-- comentários, para o balão mostrar números diferentes. O autor do comentário k é o usuário
-- seguinte ao autor do post, então ninguém comenta o próprio post. Os comentários ficam
-- depois do post, com 20 minutos entre um e outro.

INSERT INTO post (
    id,
    autor_id,
    post_pai_id,
    conteudo,
    criado_em,
    atualizado_em
)
SELECT
    md5('comentario-comunidade-' || c || '-' || p || '-' || k)::uuid,
    ('00000000-0000-0000-0000-' ||
        lpad((((c * 2 + p - 2 + k) % 10) + 1)::text, 12, '0'))::uuid,
    md5('post-comunidade-' || c || '-' || p)::uuid,
    CASE k
        WHEN 1 THEN
            'Concordo muito com isso! Passei pela mesma coisa e foi o que me ajudou a manter o ritmo.'
        WHEN 2 THEN
            'Boa reflexão. Vou tentar seguir essa ideia nas próximas leituras e conto aqui como foi.'
        ELSE
            'Adorei o post. Se tiver mais recomendações nessa linha, compartilha com a gente!'
    END,
    now() - ((p * 2 + c)::text || ' hours')::interval + ((k * 20)::text || ' minutes')::interval,
    now() - ((p * 2 + c)::text || ' hours')::interval + ((k * 20)::text || ' minutes')::interval
FROM generate_series(1, 5) AS c
CROSS JOIN generate_series(1, 3) AS p
CROSS JOIN generate_series(1, 3) AS k
WHERE k <= 4 - p;

-- Respostas, para a thread aparecer: no primeiro comentário (k = 1) do post mais recente
-- (p = 1) de cada comunidade, o autor do post responde, e o autor do comentário responde de
-- volta. A resposta k fica abaixo do comentário que ela responde (k = 1 responde o comentário,
-- k = 2 responde a resposta k = 1), 5 minutos depois.

INSERT INTO post (
    id,
    autor_id,
    post_pai_id,
    conteudo,
    criado_em,
    atualizado_em
)
SELECT
    md5('resposta-comunidade-' || c || '-' || k)::uuid,
    CASE k
        WHEN 1 THEN ('00000000-0000-0000-0000-' ||
            lpad((((c * 2 - 1) % 10) + 1)::text, 12, '0'))::uuid
        ELSE ('00000000-0000-0000-0000-' ||
            lpad((((c * 2) % 10) + 1)::text, 12, '0'))::uuid
    END,
    CASE k
        WHEN 1 THEN md5('comentario-comunidade-' || c || '-1-1')::uuid
        ELSE md5('resposta-comunidade-' || c || '-1')::uuid
    END,
    CASE k
        WHEN 1 THEN
            'Que bom que ajudou! Qual foi o último livro curto que você leu nessa rotina?'
        ELSE
            'Um de contos, foi ótimo para recomeçar. Depois te passo o nome!'
    END,
    now() - ((2 + c)::text || ' hours')::interval + ((20 + k * 5)::text || ' minutes')::interval,
    now() - ((2 + c)::text || ' hours')::interval + ((20 + k * 5)::text || ' minutes')::interval
FROM generate_series(1, 5) AS c
CROSS JOIN generate_series(1, 2) AS k;
