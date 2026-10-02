-- Troca os placeholders (placehold.co) das conquistas por ícones reais.
-- Fonte: Twemoji via jsDelivr (CDN estável, CC-BY 4.0) — https://github.com/jdecked/twemoji

UPDATE conquista SET icone_url = 'https://cdn.jsdelivr.net/gh/jdecked/twemoji@latest/assets/72x72/1f463.png'
WHERE id = 'a0000000-0000-0000-0000-000000000001'; -- Primeiros Passos -> pegadas

UPDATE conquista SET icone_url = 'https://cdn.jsdelivr.net/gh/jdecked/twemoji@latest/assets/72x72/1f4da.png'
WHERE id = 'a0000000-0000-0000-0000-000000000002'; -- Leitor Dedicado -> pilha de livros

UPDATE conquista SET icone_url = 'https://cdn.jsdelivr.net/gh/jdecked/twemoji@latest/assets/72x72/1f4d6.png'
WHERE id = 'a0000000-0000-0000-0000-000000000003'; -- Mil Páginas -> livro aberto

UPDATE conquista SET icone_url = 'https://cdn.jsdelivr.net/gh/jdecked/twemoji@latest/assets/72x72/1f91d.png'
WHERE id = 'a0000000-0000-0000-0000-000000000004'; -- Em Boa Companhia -> aperto de mãos

UPDATE conquista SET icone_url = 'https://cdn.jsdelivr.net/gh/jdecked/twemoji@latest/assets/72x72/1f3c6.png'
WHERE id = 'a0000000-0000-0000-0000-000000000005'; -- Competidor -> troféu

UPDATE conquista SET icone_url = 'https://cdn.jsdelivr.net/gh/jdecked/twemoji@latest/assets/72x72/1f451.png'
WHERE id = 'a0000000-0000-0000-0000-000000000006'; -- Lenda da Leitura -> coroa

-- Troca os placeholders (placehold.co) das capas dos clubes do livro por imagens reais.
-- Fonte: Wikimedia Commons (domínio público / licença livre).

UPDATE clube_do_livro SET capa_url = 'https://upload.wikimedia.org/wikipedia/commons/8/84/Retrato_de_Machado_de_Assis_%281905%29%2C_por_H._Bernardelli.jpg'
WHERE id = '50000000-0000-0000-0000-000000000001'; -- Clube dos Clássicos Brasileiros -> retrato de Machado de Assis (1905)

UPDATE clube_do_livro SET capa_url = 'https://upload.wikimedia.org/wikipedia/commons/e/e1/Dystopian_cyberpunk_city.png'
WHERE id = '50000000-0000-0000-0000-000000000002'; -- Distopias em Debate -> cidade cyberpunk distópica

UPDATE clube_do_livro SET capa_url = 'https://upload.wikimedia.org/wikipedia/commons/f/fc/Hokusai_Dragon.jpg'
WHERE id = '50000000-0000-0000-0000-000000000003'; -- Jornada pela Fantasia -> dragão de Hokusai

UPDATE clube_do_livro SET capa_url = 'https://upload.wikimedia.org/wikipedia/commons/8/89/Frank_Bernard_Dicksee_-_Romeo_and_Juliet%2C_1884.jpg'
WHERE id = '50000000-0000-0000-0000-000000000004'; -- Grandes Romances -> "Romeu e Julieta" (Dicksee, 1884)

UPDATE clube_do_livro SET capa_url = 'https://upload.wikimedia.org/wikipedia/commons/0/03/Woman_reading_a_book_on_lap_%28Unsplash%29.jpg'
WHERE id = '50000000-0000-0000-0000-000000000005'; -- Leituras Contemporâneas -> foto de leitura atual

-- Troca os placeholders (placehold.co) das imagens das comunidades por imagens reais.
-- Fonte: Wikimedia Commons (domínio público / licença livre).

UPDATE comunidade SET imagem_url = 'https://upload.wikimedia.org/wikipedia/commons/c/cd/Biblioteca_Nacional_do_Brasil_-_Rio_de_Janeiro_-_20220824185917.jpg'
WHERE id = '60000000-0000-0000-0000-000000000001'; -- Literatura Brasileira -> fachada da Biblioteca Nacional do Brasil

UPDATE comunidade SET imagem_url = 'https://upload.wikimedia.org/wikipedia/commons/9/9f/Verne_-_Voyage_au_centre_de_la_Terre%2C_page_29.png'
WHERE id = '60000000-0000-0000-0000-000000000002'; -- Ficção Científica -> ilustração de "Viagem ao Centro da Terra" (Verne)

UPDATE comunidade SET imagem_url = 'https://upload.wikimedia.org/wikipedia/commons/4/4d/Don_Quijote_Illustration_by_Gustave_Dore_VII.jpg'
WHERE id = '60000000-0000-0000-0000-000000000003'; -- Fantasia e Aventura -> ilustração de Dom Quixote (Gustave Doré)

UPDATE comunidade SET imagem_url = 'https://upload.wikimedia.org/wikipedia/commons/1/1c/Old_books_%28Unsplash%29.jpg'
WHERE id = '60000000-0000-0000-0000-000000000004'; -- Clássicos da Literatura -> foto de livros antigos

UPDATE comunidade SET imagem_url = 'https://upload.wikimedia.org/wikipedia/commons/1/1b/Minimal_Reading_List_%28Unsplash%29.jpg'
WHERE id = '60000000-0000-0000-0000-000000000005'; -- Leitores de Plantão -> foto minimalista de leitura
