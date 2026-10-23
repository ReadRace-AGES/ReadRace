-- Capas salvas do Google Books vieram em http://, que o Android bloqueia no app instalado (APK):
-- a capa não aparecia. O Google serve as mesmas imagens em https. Daqui para a frente o backend já
-- grava em https (ImageLinks); aqui corrige o que já está salvo. O clube copia a capa do livro ao
-- ser criado, então leva a mesma correção.

UPDATE livro
SET capa_url = 'https://' || substring(capa_url FROM 8)
WHERE capa_url LIKE 'http://books.google.%';

UPDATE clube_do_livro
SET capa_url = 'https://' || substring(capa_url FROM 8)
WHERE capa_url LIKE 'http://books.google.%';
