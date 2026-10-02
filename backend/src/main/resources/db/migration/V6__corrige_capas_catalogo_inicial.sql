-- Corrige as capas de três livros da V4 cujas URLs por ISBN devolvem um arquivo vazio.
-- Altera apenas capa_url; o ISBN do livro continua o mesmo.
-- Só atualiza as linhas que ainda possuem a URL original do seed.
UPDATE livro AS l
SET capa_url = seed.capa_nova
FROM (VALUES
    ('30000000-0000-0000-0000-000000000001'::uuid,
     'https://covers.openlibrary.org/b/isbn/9788535910663-L.jpg',
     'https://covers.openlibrary.org/b/id/647501-L.jpg'),
    ('30000000-0000-0000-0000-000000000017'::uuid,
     'https://covers.openlibrary.org/b/isbn/9788535911662-L.jpg',
     'https://covers.openlibrary.org/b/id/123152-L.jpg'),
    ('30000000-0000-0000-0000-000000000018'::uuid,
     'https://covers.openlibrary.org/b/isbn/9780007525546-L.jpg',
     'https://covers.openlibrary.org/b/id/11261770-L.jpg')
) AS seed(id, capa_antiga, capa_nova)
WHERE l.id = seed.id
  AND l.capa_url = seed.capa_antiga;
