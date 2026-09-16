-- ============================================================
-- SEED DO BANCO DE DADOS - READRACE
-- Task #13
--
-- Este arquivo popula o schema criado pelas migrations
-- anteriores com dados determinísticos para a Sprint 1.
-- ============================================================


-- ============================================================
-- 1. USUÁRIOS
-- ============================================================

INSERT INTO usuario (
    id,
    nome,
    nome_usuario,
    email,
    avatar_url,
    xp_total,
    meta_frequencia,
    meta_paginas,
    dias_consecutivos,
    ultima_leitura_em,
    nivel
) VALUES
(
    '00000000-0000-0000-0000-000000000001',
    'Daniel Ribeiro',
    'danielribeiro',
    'daniel@readrace.com',
    'https://i.pravatar.cc/300?img=12',
    2450, 5, 120, 12, CURRENT_DATE - 1, 5
),
(
    '00000000-0000-0000-0000-000000000002',
    'Ana Silva',
    'anasilva',
    'ana@readrace.com',
    'https://i.pravatar.cc/300?img=47',
    1850, 4, 100, 8, CURRENT_DATE, 4
),
(
    '00000000-0000-0000-0000-000000000003',
    'Lucas Martins',
    'lucasmartins',
    'lucas@readrace.com',
    'https://i.pravatar.cc/300?img=11',
    1320, 3, 80, 5, CURRENT_DATE - 2, 3
),
(
    '00000000-0000-0000-0000-000000000004',
    'Mariana Costa',
    'marianacosta',
    'mariana@readrace.com',
    'https://i.pravatar.cc/300?img=45',
    980, 4, 60, 4, CURRENT_DATE - 3, 3
),
(
    '00000000-0000-0000-0000-000000000005',
    'Gabriel Souza',
    'gabrielsouza',
    'gabriel@readrace.com',
    'https://i.pravatar.cc/300?img=13',
    760, 3, 50, 3, CURRENT_DATE - 1, 2
),
(
    '00000000-0000-0000-0000-000000000006',
    'Beatriz Oliveira',
    'beatrizoliveira',
    'beatriz@readrace.com',
    'https://i.pravatar.cc/300?img=44',
    620, 5, 70, 6, CURRENT_DATE, 2
),
(
    '00000000-0000-0000-0000-000000000007',
    'Rafael Lima',
    'rafaellima',
    'rafael@readrace.com',
    'https://i.pravatar.cc/300?img=14',
    480, 3, 40, 2, CURRENT_DATE - 4, 2
),
(
    '00000000-0000-0000-0000-000000000008',
    'Camila Rocha',
    'camilarocha',
    'camila@readrace.com',
    'https://i.pravatar.cc/300?img=43',
    310, 2, 30, 1, CURRENT_DATE - 5, 1
),
(
    '00000000-0000-0000-0000-000000000009',
    'Pedro Almeida',
    'pedroalmeida',
    'pedro@readrace.com',
    'https://i.pravatar.cc/300?img=15',
    190, 3, 25, 1, CURRENT_DATE - 6, 1
),
(
    '00000000-0000-0000-0000-000000000010',
    'Julia Ferreira',
    'juliaferreira',
    'julia@readrace.com',
    'https://i.pravatar.cc/300?img=42',
    90, 2, 20, 0, NULL, 1
);


-- ============================================================
-- 2. AUTORES
-- ============================================================

INSERT INTO autor (id, nome) VALUES
('10000000-0000-0000-0000-000000000001', 'Machado de Assis'),
('10000000-0000-0000-0000-000000000002', 'George Orwell'),
('10000000-0000-0000-0000-000000000003', 'Jane Austen'),
('10000000-0000-0000-0000-000000000004', 'Franz Kafka'),
('10000000-0000-0000-0000-000000000005', 'Mary Shelley'),
('10000000-0000-0000-0000-000000000006', 'Fiódor Dostoiévski'),
('10000000-0000-0000-0000-000000000007', 'Clarice Lispector'),
('10000000-0000-0000-0000-000000000008', 'J. R. R. Tolkien'),
('10000000-0000-0000-0000-000000000009', 'Antoine de Saint-Exupéry'),
('10000000-0000-0000-0000-000000000010', 'Aldous Huxley'),
('10000000-0000-0000-0000-000000000011', 'Oscar Wilde'),
('10000000-0000-0000-0000-000000000012', 'José Saramago'),
('10000000-0000-0000-0000-000000000013', 'Gabriel García Márquez'),
('10000000-0000-0000-0000-000000000014', 'Virginia Woolf'),
('10000000-0000-0000-0000-000000000015', 'Hermann Hesse');


-- ============================================================
-- 3. GÊNEROS
-- ============================================================

INSERT INTO genero (id, nome) VALUES
('20000000-0000-0000-0000-000000000001', 'Romance'),
('20000000-0000-0000-0000-000000000002', 'Ficção Científica'),
('20000000-0000-0000-0000-000000000003', 'Fantasia'),
('20000000-0000-0000-0000-000000000004', 'Clássico'),
('20000000-0000-0000-0000-000000000005', 'Distopia'),
('20000000-0000-0000-0000-000000000006', 'Terror'),
('20000000-0000-0000-0000-000000000007', 'Filosofia'),
('20000000-0000-0000-0000-000000000008', 'Realismo'),
('20000000-0000-0000-0000-000000000009', 'Literatura Brasileira'),
('20000000-0000-0000-0000-000000000010', 'Aventura');


-- ============================================================
-- 4. LIVROS
-- ============================================================

INSERT INTO livro (
    id,
    isbn,
    titulo,
    subtitulo,
    total_paginas,
    capa_url,
    data_publicacao
) VALUES
(
    '30000000-0000-0000-0000-000000000001',
    '9788535910663',
    'Dom Casmurro',
    NULL,
    256,
    'https://covers.openlibrary.org/b/isbn/9788535910663-L.jpg',
    '1899-01-01'
),
(
    '30000000-0000-0000-0000-000000000002',
    '9780451524935',
    '1984',
    NULL,
    328,
    'https://covers.openlibrary.org/b/isbn/9780451524935-L.jpg',
    '1949-06-08'
),
(
    '30000000-0000-0000-0000-000000000003',
    '9780141439518',
    'Orgulho e Preconceito',
    NULL,
    432,
    'https://covers.openlibrary.org/b/isbn/9780141439518-L.jpg',
    '1813-01-28'
),
(
    '30000000-0000-0000-0000-000000000004',
    '9780805209990',
    'A Metamorfose',
    NULL,
    128,
    'https://covers.openlibrary.org/b/isbn/9780805209990-L.jpg',
    '1915-01-01'
),
(
    '30000000-0000-0000-0000-000000000005',
    '9780486282114',
    'Frankenstein',
    NULL,
    280,
    'https://covers.openlibrary.org/b/isbn/9780486282114-L.jpg',
    '1818-01-01'
),
(
    '30000000-0000-0000-0000-000000000006',
    '9780140449136',
    'Crime e Castigo',
    NULL,
    720,
    'https://covers.openlibrary.org/b/isbn/9780140449136-L.jpg',
    '1866-01-01'
),
(
    '30000000-0000-0000-0000-000000000007',
    '9788532508126',
    'A Hora da Estrela',
    NULL,
    88,
    'https://covers.openlibrary.org/b/isbn/9788532508126-L.jpg',
    '1977-01-01'
),
(
    '30000000-0000-0000-0000-000000000008',
    '9780547928227',
    'O Hobbit',
    NULL,
    300,
    'https://covers.openlibrary.org/b/isbn/9780547928227-L.jpg',
    '1937-09-21'
),
(
    '30000000-0000-0000-0000-000000000009',
    '9780156012195',
    'O Pequeno Príncipe',
    NULL,
    96,
    'https://covers.openlibrary.org/b/isbn/9780156012195-L.jpg',
    '1943-04-06'
),
(
    '30000000-0000-0000-0000-000000000010',
    '9780060850524',
    'Admirável Mundo Novo',
    NULL,
    288,
    'https://covers.openlibrary.org/b/isbn/9780060850524-L.jpg',
    '1932-01-01'
),
(
    '30000000-0000-0000-0000-000000000011',
    '9780141439570',
    'O Retrato de Dorian Gray',
    NULL,
    304,
    'https://covers.openlibrary.org/b/isbn/9780141439570-L.jpg',
    '1890-01-01'
),
(
    '30000000-0000-0000-0000-000000000012',
    '9780156007757',
    'Ensaio sobre a Cegueira',
    NULL,
    352,
    'https://covers.openlibrary.org/b/isbn/9780156007757-L.jpg',
    '1995-01-01'
),
(
    '30000000-0000-0000-0000-000000000013',
    '9780060883287',
    'Cem Anos de Solidão',
    NULL,
    417,
    'https://covers.openlibrary.org/b/isbn/9780060883287-L.jpg',
    '1967-05-30'
),
(
    '30000000-0000-0000-0000-000000000014',
    '9780156628709',
    'Mrs Dalloway',
    NULL,
    216,
    'https://covers.openlibrary.org/b/isbn/9780156628709-L.jpg',
    '1925-05-14'
),
(
    '30000000-0000-0000-0000-000000000015',
    '9780553208849',
    'Sidarta',
    NULL,
    160,
    'https://covers.openlibrary.org/b/isbn/9780553208849-L.jpg',
    '1922-01-01'
),
(
    '30000000-0000-0000-0000-000000000016',
    '9780140449242',
    'Os Irmãos Karamázov',
    NULL,
    824,
    'https://covers.openlibrary.org/b/isbn/9780140449242-L.jpg',
    '1880-01-01'
),
(
    '30000000-0000-0000-0000-000000000017',
    '9788535911662',
    'Memórias Póstumas de Brás Cubas',
    NULL,
    208,
    'https://covers.openlibrary.org/b/isbn/9788535911662-L.jpg',
    '1881-01-01'
),
(
    '30000000-0000-0000-0000-000000000018',
    '9780007525546',
    'A Revolução dos Bichos',
    NULL,
    112,
    'https://covers.openlibrary.org/b/isbn/9780007525546-L.jpg',
    '1945-08-17'
),
(
    '30000000-0000-0000-0000-000000000019',
    '9780544003415',
    'O Senhor dos Anéis',
    'A Sociedade do Anel',
    432,
    'https://covers.openlibrary.org/b/isbn/9780544003415-L.jpg',
    '1954-07-29'
),
(
    '30000000-0000-0000-0000-000000000020',
    '9788535914849',
    'A Paixão Segundo G.H.',
    NULL,
    192,
    'https://covers.openlibrary.org/b/isbn/9788535914849-L.jpg',
    '1964-01-01'
);


-- ============================================================
-- 5. LIVRO - AUTOR
-- ============================================================

INSERT INTO livro_autor (livro_id, autor_id, ordem) VALUES
('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 1),
('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', 1),
('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000003', 1),
('30000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000004', 1),
('30000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000005', 1),
('30000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000006', 1),
('30000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000007', 1),
('30000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000008', 1),
('30000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000009', 1),
('30000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000010', 1),
('30000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000011', 1),
('30000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000012', 1),
('30000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000013', 1),
('30000000-0000-0000-0000-000000000014', '10000000-0000-0000-0000-000000000014', 1),
('30000000-0000-0000-0000-000000000015', '10000000-0000-0000-0000-000000000015', 1),
('30000000-0000-0000-0000-000000000016', '10000000-0000-0000-0000-000000000006', 1),
('30000000-0000-0000-0000-000000000017', '10000000-0000-0000-0000-000000000001', 1),
('30000000-0000-0000-0000-000000000018', '10000000-0000-0000-0000-000000000002', 1),
('30000000-0000-0000-0000-000000000019', '10000000-0000-0000-0000-000000000008', 1),
('30000000-0000-0000-0000-000000000020', '10000000-0000-0000-0000-000000000007', 1);


-- ============================================================
-- 6. LIVRO - GÊNERO
-- ============================================================

INSERT INTO livro_genero (livro_id, genero_id) VALUES
('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000004'),
('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000009'),
('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002'),
('30000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000005'),
('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000001'),
('30000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000004'),
('30000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000004'),
('30000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000006'),
('30000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000004'),
('30000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000009'),
('30000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000003'),
('30000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000010'),
('30000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000010'),
('30000000-0000-0000-0000-000000000010', '20000000-0000-0000-0000-000000000005'),
('30000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000004'),
('30000000-0000-0000-0000-000000000012', '20000000-0000-0000-0000-000000000001'),
('30000000-0000-0000-0000-000000000013', '20000000-0000-0000-0000-000000000001'),
('30000000-0000-0000-0000-000000000014', '20000000-0000-0000-0000-000000000001'),
('30000000-0000-0000-0000-000000000015', '20000000-0000-0000-0000-000000000007'),
('30000000-0000-0000-0000-000000000016', '20000000-0000-0000-0000-000000000004'),
('30000000-0000-0000-0000-000000000017', '20000000-0000-0000-0000-000000000008'),
('30000000-0000-0000-0000-000000000017', '20000000-0000-0000-0000-000000000009'),
('30000000-0000-0000-0000-000000000018', '20000000-0000-0000-0000-000000000005'),
('30000000-0000-0000-0000-000000000019', '20000000-0000-0000-0000-000000000003'),
('30000000-0000-0000-0000-000000000019', '20000000-0000-0000-0000-000000000010'),
('30000000-0000-0000-0000-000000000020', '20000000-0000-0000-0000-000000000009');


-- ============================================================
-- 7. BIBLIOTECA DO USUÁRIO FIXO
--
-- 12 livros
-- lendo, lido e desejo
-- favorito funciona como flag
-- exatamente 3 favoritos
-- ============================================================

INSERT INTO item_biblioteca (
    id,
    usuario_id,
    livro_id,
    status_leitura,
    favorito,
    pagina_atual,
    pagina_maxima,
    adicionado_em
) VALUES
('40000000-0000-0000-0000-000000000001',
 '00000000-0000-0000-0000-000000000001',
 '30000000-0000-0000-0000-000000000001',
 'lendo', TRUE, 145, 256, now() - INTERVAL '30 days'),

('40000000-0000-0000-0000-000000000002',
 '00000000-0000-0000-0000-000000000001',
 '30000000-0000-0000-0000-000000000002',
 'lendo', TRUE, 210, 328, now() - INTERVAL '24 days'),

('40000000-0000-0000-0000-000000000003',
 '00000000-0000-0000-0000-000000000001',
 '30000000-0000-0000-0000-000000000003',
 'lido', TRUE, 432, 432, now() - INTERVAL '90 days'),

('40000000-0000-0000-0000-000000000004',
 '00000000-0000-0000-0000-000000000001',
 '30000000-0000-0000-0000-000000000004',
 'lido', FALSE, 128, 128, now() - INTERVAL '75 days'),

('40000000-0000-0000-0000-000000000005',
 '00000000-0000-0000-0000-000000000001',
 '30000000-0000-0000-0000-000000000005',
 'desejo', FALSE, 0, 280, now() - INTERVAL '10 days'),

('40000000-0000-0000-0000-000000000006',
 '00000000-0000-0000-0000-000000000001',
 '30000000-0000-0000-0000-000000000006',
 'lendo', FALSE, 310, 720, now() - INTERVAL '20 days'),

('40000000-0000-0000-0000-000000000007',
 '00000000-0000-0000-0000-000000000001',
 '30000000-0000-0000-0000-000000000007',
 'lido', FALSE, 88, 88, now() - INTERVAL '50 days'),

('40000000-0000-0000-0000-000000000008',
 '00000000-0000-0000-0000-000000000001',
 '30000000-0000-0000-0000-000000000008',
 'desejo', FALSE, 0, 300, now() - INTERVAL '8 days'),

('40000000-0000-0000-0000-000000000009',
 '00000000-0000-0000-0000-000000000001',
 '30000000-0000-0000-0000-000000000009',
 'lido', FALSE, 96, 96, now() - INTERVAL '120 days'),

('40000000-0000-0000-0000-000000000010',
 '00000000-0000-0000-0000-000000000001',
 '30000000-0000-0000-0000-000000000010',
 'lendo', FALSE, 94, 288, now() - INTERVAL '15 days'),

('40000000-0000-0000-0000-000000000011',
 '00000000-0000-0000-0000-000000000001',
 '30000000-0000-0000-0000-000000000011',
 'desejo', FALSE, 0, 304, now() - INTERVAL '5 days'),

('40000000-0000-0000-0000-000000000012',
 '00000000-0000-0000-0000-000000000001',
 '30000000-0000-0000-0000-000000000012',
 'lido', FALSE, 352, 352, now() - INTERVAL '60 days');


-- ============================================================
-- 8. REGISTROS DE LEITURA
-- 15 registros distribuídos no tempo
-- ============================================================

INSERT INTO registro_leitura (
    id,
    item_biblioteca_id,
    ultima_pagina,
    registrado_em
) VALUES
('41000000-0000-0000-0000-000000000001',
 '40000000-0000-0000-0000-000000000001', 20, now() - INTERVAL '14 days'),

('41000000-0000-0000-0000-000000000002',
 '40000000-0000-0000-0000-000000000001', 48, now() - INTERVAL '10 days'),

('41000000-0000-0000-0000-000000000003',
 '40000000-0000-0000-0000-000000000001', 82, now() - INTERVAL '7 days'),

('41000000-0000-0000-0000-000000000004',
 '40000000-0000-0000-0000-000000000001', 115, now() - INTERVAL '3 days'),

('41000000-0000-0000-0000-000000000005',
 '40000000-0000-0000-0000-000000000001', 145, now() - INTERVAL '1 day'),

('41000000-0000-0000-0000-000000000006',
 '40000000-0000-0000-0000-000000000002', 60, now() - INTERVAL '12 days'),

('41000000-0000-0000-0000-000000000007',
 '40000000-0000-0000-0000-000000000002', 130, now() - INTERVAL '8 days'),

('41000000-0000-0000-0000-000000000008',
 '40000000-0000-0000-0000-000000000002', 210, now() - INTERVAL '2 days'),

('41000000-0000-0000-0000-000000000009',
 '40000000-0000-0000-0000-000000000006', 75, now() - INTERVAL '18 days'),

('41000000-0000-0000-0000-000000000010',
 '40000000-0000-0000-0000-000000000006', 150, now() - INTERVAL '11 days'),

('41000000-0000-0000-0000-000000000011',
 '40000000-0000-0000-0000-000000000006', 230, now() - INTERVAL '6 days'),

('41000000-0000-0000-0000-000000000012',
 '40000000-0000-0000-0000-000000000006', 310, now() - INTERVAL '1 day'),

('41000000-0000-0000-0000-000000000013',
 '40000000-0000-0000-0000-000000000010', 25, now() - INTERVAL '9 days'),

('41000000-0000-0000-0000-000000000014',
 '40000000-0000-0000-0000-000000000010', 57, now() - INTERVAL '5 days'),

('41000000-0000-0000-0000-000000000015',
 '40000000-0000-0000-0000-000000000010', 94, now() - INTERVAL '1 day');


-- ============================================================
-- 9. CLUBES DO LIVRO
-- ============================================================

INSERT INTO clube_do_livro (
    id,
    livro_id,
    nome,
    descricao,
    capa_url
) VALUES
(
    '50000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000001',
    'Clube dos Clássicos Brasileiros',
    'Leituras e conversas sobre grandes obras da literatura brasileira.',
    'https://placehold.co/600x400?text=Classicos+Brasileiros'
),
(
    '50000000-0000-0000-0000-000000000002',
    '30000000-0000-0000-0000-000000000002',
    'Distopias em Debate',
    'Um espaço para discutir distopias, política, sociedade e literatura.',
    'https://placehold.co/600x400?text=Distopias'
),
(
    '50000000-0000-0000-0000-000000000003',
    '30000000-0000-0000-0000-000000000008',
    'Jornada pela Fantasia',
    'Clube para quem gosta de mundos fantásticos, aventuras e grandes jornadas.',
    'https://placehold.co/600x400?text=Fantasia'
),
(
    '50000000-0000-0000-0000-000000000004',
    '30000000-0000-0000-0000-000000000006',
    'Grandes Romances',
    'Discussões sobre personagens complexos e romances que atravessaram gerações.',
    'https://placehold.co/600x400?text=Grandes+Romances'
),
(
    '50000000-0000-0000-0000-000000000005',
    '30000000-0000-0000-0000-000000000012',
    'Leituras Contemporâneas',
    'Encontros para conversar sobre obras modernas e seus temas sociais.',
    'https://placehold.co/600x400?text=Leituras+Contemporaneas'
);


-- ============================================================
-- 10. MEMBROS DOS CLUBES
--
-- 8 membros por clube.
-- O usuário fixo participa SOMENTE dos clubes 1 e 2.
-- ============================================================

INSERT INTO membro_clube (
    id,
    clube_id,
    usuario_id,
    pontos,
    entrou_em,
    cargo_clube
)
SELECT
    md5('membro-clube-' || c.clube_num || '-' || u.usuario_num)::uuid,
    ('50000000-0000-0000-0000-' || lpad(c.clube_num::text, 12, '0'))::uuid,
    ('00000000-0000-0000-0000-' || lpad(u.usuario_num::text, 12, '0'))::uuid,
    1200 - (u.posicao * 83) - (c.clube_num * 17),
    now() - ((u.posicao * 9 + c.clube_num)::text || ' days')::interval,
    CASE
        WHEN u.posicao = 1 THEN 'administrador'::cargo
        ELSE 'membro'::cargo
    END
FROM (
    VALUES
        (1, 1, 1), (1, 2, 2), (1, 3, 3), (1, 4, 4),
        (1, 5, 5), (1, 6, 6), (1, 7, 7), (1, 8, 8),

        (2, 1, 1), (2, 2, 2), (2, 3, 3), (2, 4, 4),
        (2, 5, 5), (2, 6, 6), (2, 7, 7), (2, 8, 8),

        (3, 2, 1), (3, 3, 2), (3, 4, 3), (3, 5, 4),
        (3, 6, 5), (3, 7, 6), (3, 8, 7), (3, 9, 8),

        (4, 3, 1), (4, 4, 2), (4, 5, 3), (4, 6, 4),
        (4, 7, 5), (4, 8, 6), (4, 9, 7), (4, 10, 8),

        (5, 2, 1), (5, 4, 2), (5, 5, 3), (5, 6, 4),
        (5, 7, 5), (5, 8, 6), (5, 9, 7), (5, 10, 8)
) AS u(clube_num, usuario_num, posicao)
JOIN (
    VALUES (1), (2), (3), (4), (5)
) AS c(clube_num)
ON c.clube_num = u.clube_num;


-- ============================================================
-- 11. METAS DOS CLUBES
-- ============================================================

INSERT INTO meta_clube (
    id,
    clube_id,
    tipo_meta,
    valor_alvo,
    inicio_em,
    fim_em
) VALUES
(
    '52000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000001',
    'paginas', 600,
    now() - INTERVAL '10 days',
    now() + INTERVAL '20 days'
),
(
    '52000000-0000-0000-0000-000000000002',
    '50000000-0000-0000-0000-000000000002',
    'paginas', 800,
    now() - INTERVAL '7 days',
    now() + INTERVAL '23 days'
),
(
    '52000000-0000-0000-0000-000000000003',
    '50000000-0000-0000-0000-000000000003',
    'livro', 1,
    now() - INTERVAL '5 days',
    now() + INTERVAL '25 days'
),
(
    '52000000-0000-0000-0000-000000000004',
    '50000000-0000-0000-0000-000000000004',
    'paginas', 1000,
    now() - INTERVAL '12 days',
    now() + INTERVAL '18 days'
),
(
    '52000000-0000-0000-0000-000000000005',
    '50000000-0000-0000-0000-000000000005',
    'livro', 2,
    now() - INTERVAL '3 days',
    now() + INTERVAL '27 days'
);


-- ============================================================
-- 12. COMUNIDADES
-- ============================================================

INSERT INTO comunidade (
    id,
    nome,
    descricao,
    imagem_url
) VALUES
(
    '60000000-0000-0000-0000-000000000001',
    'Literatura Brasileira',
    'Comunidade dedicada a autores, obras e movimentos da literatura brasileira.',
    'https://placehold.co/600x400?text=Literatura+Brasileira'
),
(
    '60000000-0000-0000-0000-000000000002',
    'Ficção Científica',
    'Discussões sobre futuros possíveis, tecnologia, sociedade e ficção científica.',
    'https://placehold.co/600x400?text=Ficcao+Cientifica'
),
(
    '60000000-0000-0000-0000-000000000003',
    'Fantasia e Aventura',
    'Para leitores que gostam de grandes jornadas, mundos fantásticos e aventuras.',
    'https://placehold.co/600x400?text=Fantasia+Aventura'
),
(
    '60000000-0000-0000-0000-000000000004',
    'Clássicos da Literatura',
    'Um espaço para descobrir e redescobrir clássicos de diferentes épocas.',
    'https://placehold.co/600x400?text=Classicos'
),
(
    '60000000-0000-0000-0000-000000000005',
    'Leitores de Plantão',
    'Comunidade aberta para compartilhar leituras atuais, metas e recomendações.',
    'https://placehold.co/600x400?text=Leitores+de+Plantao'
);


-- ============================================================
-- 13. MEMBROS DAS COMUNIDADES
-- 10 usuários em cada comunidade
-- ============================================================

INSERT INTO membro_comunidade (
    id,
    comunidade_id,
    usuario_id,
    entrou_em,
    cargo_comunidade
)
SELECT
    md5('membro-comunidade-' || c || '-' || u)::uuid,
    ('60000000-0000-0000-0000-' || lpad(c::text, 12, '0'))::uuid,
    ('00000000-0000-0000-0000-' || lpad(u::text, 12, '0'))::uuid,
    now() - ((c * 5 + u * 3)::text || ' days')::interval,
    CASE
        WHEN u = c THEN 'administrador'::cargo
        ELSE 'membro'::cargo
    END
FROM generate_series(1, 5) AS c
CROSS JOIN generate_series(1, 10) AS u;


-- ============================================================
-- 14. POSTS DOS CLUBES
-- 6 posts em cada clube
-- ============================================================

INSERT INTO post (
    id,
    autor_id,
    clube_id,
    conteudo,
    criado_em,
    atualizado_em
)
SELECT
    md5('post-clube-' || c || '-' || p)::uuid,
    ('00000000-0000-0000-0000-' ||
        lpad((((c + p - 2) % 8) + 1)::text, 12, '0'))::uuid,
    ('50000000-0000-0000-0000-' || lpad(c::text, 12, '0'))::uuid,
    CASE p
        WHEN 1 THEN
            'Comecei a leitura desta semana e já encontrei vários detalhes que tinham passado despercebidos na primeira vez. A construção dos personagens está tornando a discussão do clube muito mais interessante.'
        WHEN 2 THEN
            'Uma coisa que mais me chamou atenção neste trecho foi a maneira como o autor transforma uma situação aparentemente simples em uma reflexão muito maior. Quero saber como vocês interpretaram essa parte.'
        WHEN 3 THEN
            'A leitura está avançando melhor do que eu esperava. Algumas passagens exigem mais atenção, mas justamente por isso estou gostando de acompanhar as opiniões diferentes que aparecem aqui no clube.'
        WHEN 4 THEN
            'Cheguei ao capítulo de hoje e fiquei pensando bastante sobre as decisões dos personagens. É interessante perceber como uma mesma cena pode produzir interpretações completamente diferentes entre os leitores.'
        WHEN 5 THEN
            'Minha impressão até agora mudou bastante desde o início do livro. Alguns personagens que pareciam simples começaram a ganhar novas camadas, e isso deixou a leitura muito mais envolvente para mim.'
        ELSE
            'Terminei a meta de leitura desta etapa e gostei muito da experiência de acompanhar o livro em grupo. Ler os comentários de vocês acrescentou perspectivas que eu provavelmente não teria percebido sozinho.'
    END,
    now() - ((p + c)::text || ' days')::interval,
    now() - ((p + c)::text || ' days')::interval
FROM generate_series(1, 5) AS c
CROSS JOIN generate_series(1, 6) AS p;


-- ============================================================
-- 15. POSTS DAS COMUNIDADES
-- 6 posts em cada comunidade
-- ============================================================

INSERT INTO post (
    id,
    autor_id,
    comunidade_id,
    conteudo,
    criado_em,
    atualizado_em
)
SELECT
    md5('post-comunidade-' || c || '-' || p)::uuid,
    ('00000000-0000-0000-0000-' ||
        lpad((((c * 2 + p - 2) % 10) + 1)::text, 12, '0'))::uuid,
    ('60000000-0000-0000-0000-' || lpad(c::text, 12, '0'))::uuid,
    CASE p
        WHEN 1 THEN
            'Quero deixar uma recomendação para quem está procurando uma próxima leitura. Tenho tentado alternar livros mais longos com obras curtas e isso tornou minha rotina de leitura muito mais constante.'
        WHEN 2 THEN
            'Vocês também percebem que a experiência de reler um livro muda completamente depois de alguns anos? Estou revisitando algumas obras e encontrando ideias que eu simplesmente não tinha percebido antes.'
        WHEN 3 THEN
            'Minha meta para este mês é manter uma rotina pequena, mas consistente. Em vez de tentar ler muitas páginas de uma vez, estou reservando um pouco de tempo todos os dias e o resultado tem sido ótimo.'
        WHEN 4 THEN
            'Uma das coisas que mais gosto nesta comunidade é encontrar livros que provavelmente não descobriria sozinho. Já adicionei várias recomendações à minha lista e quero saber quais foram as melhores leituras de vocês neste ano.'
        WHEN 5 THEN
            'Acabei de terminar uma leitura que me surpreendeu bastante. Entrei sem grandes expectativas, mas os personagens e a construção da história foram ficando cada vez mais interessantes conforme avancei.'
        ELSE
            'Estou organizando minha próxima sequência de leituras e queria ouvir sugestões. Procuro algo envolvente, com bons personagens e que renda bastante discussão depois que a leitura terminar.'
    END,
    now() - ((p * 2 + c)::text || ' hours')::interval,
    now() - ((p * 2 + c)::text || ' hours')::interval
FROM generate_series(1, 5) AS c
CROSS JOIN generate_series(1, 6) AS p;


-- ============================================================
-- 16. POSTS SOBRE LIVROS
-- Necessários para a tela de detalhe do livro
-- ============================================================

INSERT INTO post (
    id,
    autor_id,
    livro_id,
    conteudo,
    criado_em,
    atualizado_em
) VALUES
(
    '72000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002',
    '30000000-0000-0000-0000-000000000001',
    'A forma como a memória do narrador organiza os acontecimentos é uma das partes mais interessantes desta leitura. Quanto mais avanço, mais fico em dúvida sobre quais lembranças devem ser aceitas sem questionamento.',
    now() - INTERVAL '2 days',
    now() - INTERVAL '2 days'
),
(
    '72000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000003',
    '30000000-0000-0000-0000-000000000001',
    'Estou relendo este livro e a experiência está sendo completamente diferente. Pequenos detalhes das conversas ganham outro significado quando já conhecemos o caminho que a história vai seguir.',
    now() - INTERVAL '1 day',
    now() - INTERVAL '1 day'
),
(
    '72000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000004',
    '30000000-0000-0000-0000-000000000002',
    'É impressionante como vários temas apresentados no livro continuam rendendo discussões atuais. A leitura é desconfortável em alguns momentos, mas justamente por isso provoca tantas reflexões.',
    now() - INTERVAL '4 days',
    now() - INTERVAL '4 days'
),
(
    '72000000-0000-0000-0000-000000000004',
    '00000000-0000-0000-0000-000000000006',
    '30000000-0000-0000-0000-000000000008',
    'O ritmo da aventura funciona muito bem e o mundo vai ficando maior a cada capítulo. Entendo por que tanta gente começa por este livro antes de conhecer as outras histórias do universo.',
    now() - INTERVAL '3 days',
    now() - INTERVAL '3 days'
),
(
    '72000000-0000-0000-0000-000000000005',
    '00000000-0000-0000-0000-000000000007',
    '30000000-0000-0000-0000-000000000006',
    'Não é uma leitura rápida, mas a construção psicológica dos personagens recompensa bastante a atenção. Algumas conversas permanecem na cabeça mesmo depois de fechar o livro.',
    now() - INTERVAL '5 days',
    now() - INTERVAL '5 days'
),
(
    '72000000-0000-0000-0000-000000000006',
    '00000000-0000-0000-0000-000000000008',
    '30000000-0000-0000-0000-000000000012',
    'A ideia central do livro é forte e a maneira como os personagens reagem aos acontecimentos torna tudo ainda mais intenso. Foi uma leitura que me fez pensar bastante depois de terminar.',
    now() - INTERVAL '6 days',
    now() - INTERVAL '6 days'
);


-- ============================================================
-- 17. CURTIDAS
-- Quantidades variadas para os contadores
-- ============================================================

INSERT INTO curtida (
    id,
    post_id,
    usuario_id,
    criada_em
)
SELECT
    md5('curtida-clube-' || c || '-' || p || '-' || u)::uuid,
    md5('post-clube-' || c || '-' || p)::uuid,
    ('00000000-0000-0000-0000-' || lpad(u::text, 12, '0'))::uuid,
    now() - ((c + p + u)::text || ' hours')::interval
FROM generate_series(1, 5) AS c
CROSS JOIN generate_series(1, 6) AS p
CROSS JOIN generate_series(1, 10) AS u
WHERE u <= ((c + p) % 7) + 1;


INSERT INTO curtida (
    id,
    post_id,
    usuario_id,
    criada_em
)
SELECT
    md5('curtida-comunidade-' || c || '-' || p || '-' || u)::uuid,
    md5('post-comunidade-' || c || '-' || p)::uuid,
    ('00000000-0000-0000-0000-' || lpad(u::text, 12, '0'))::uuid,
    now() - ((c + p + u)::text || ' hours')::interval
FROM generate_series(1, 5) AS c
CROSS JOIN generate_series(1, 6) AS p
CROSS JOIN generate_series(1, 10) AS u
WHERE u <= ((c * 2 + p) % 8) + 1;


-- ============================================================
-- 18. SEGUIR
--
-- Usuário fixo possui relações recíprocas.
-- Esses usuários podem ser usados como oponentes.
-- ============================================================

INSERT INTO seguir (
    id,
    seguidor_id,
    seguido_id,
    criado_em
) VALUES
(
    '80000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002',
    now() - INTERVAL '90 days'
),
(
    '80000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    now() - INTERVAL '89 days'
),
(
    '80000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000003',
    now() - INTERVAL '80 days'
),
(
    '80000000-0000-0000-0000-000000000004',
    '00000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000001',
    now() - INTERVAL '79 days'
),
(
    '80000000-0000-0000-0000-000000000005',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000004',
    now() - INTERVAL '70 days'
),
(
    '80000000-0000-0000-0000-000000000006',
    '00000000-0000-0000-0000-000000000004',
    '00000000-0000-0000-0000-000000000001',
    now() - INTERVAL '69 days'
),
(
    '80000000-0000-0000-0000-000000000007',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000005',
    now() - INTERVAL '60 days'
),
(
    '80000000-0000-0000-0000-000000000008',
    '00000000-0000-0000-0000-000000000005',
    '00000000-0000-0000-0000-000000000001',
    now() - INTERVAL '59 days'
),
(
    '80000000-0000-0000-0000-000000000009',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000003',
    now() - INTERVAL '50 days'
),
(
    '80000000-0000-0000-0000-000000000010',
    '00000000-0000-0000-0000-000000000006',
    '00000000-0000-0000-0000-000000000002',
    now() - INTERVAL '40 days'
);


-- ============================================================
-- 19. DESAFIOS
--
-- 1 ativo
-- 1 finalizado ganho pelo usuário fixo
-- 1 finalizado perdido pelo usuário fixo
-- 1 pendente
-- ============================================================

INSERT INTO desafio_amigo (
    id,
    criador_id,
    oponente_id,
    livro_id,
    titulo,
    descricao,
    tipo_meta,
    meta_valor,
    recompensa_xp,
    dificuldade,
    icone_url,
    status,
    inicio_em,
    fim_em,
    criado_em
) VALUES
(
    '90000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002',
    NULL,
    'Corrida das 300 páginas',
    'Quem consegue ler 300 páginas primeiro?',
    'paginas',
    300,
    150,
    'medio',
    'https://placehold.co/128x128?text=300',
    'ativo',
    now() - INTERVAL '5 days',
    now() + INTERVAL '9 days',
    now() - INTERVAL '6 days'
),
(
    '90000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000001',
    NULL,
    'Maratona de leitura',
    'Uma disputa de páginas durante duas semanas.',
    'paginas',
    300,
    200,
    'dificil',
    'https://placehold.co/128x128?text=Maratona',
    'finalizado',
    now() - INTERVAL '30 days',
    now() - INTERVAL '16 days',
    now() - INTERVAL '31 days'
),
(
    '90000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000004',
    '30000000-0000-0000-0000-000000000004',
    'Desafio A Metamorfose',
    'Quem termina A Metamorfose primeiro?',
    'livro',
    1,
    180,
    'medio',
    'https://placehold.co/128x128?text=Livro',
    'finalizado',
    now() - INTERVAL '50 days',
    now() - INTERVAL '40 days',
    now() - INTERVAL '51 days'
),
(
    '90000000-0000-0000-0000-000000000004',
    '00000000-0000-0000-0000-000000000005',
    '00000000-0000-0000-0000-000000000001',
    NULL,
    'Desafio de fim de semana',
    'Vamos ver quem lê mais páginas no próximo fim de semana.',
    'paginas',
    150,
    100,
    'facil',
    'https://placehold.co/128x128?text=Weekend',
    'pendente',
    now() + INTERVAL '2 days',
    now() + INTERVAL '5 days',
    now()
);


-- ============================================================
-- 20. PROGRESSO DOS DESAFIOS
-- Dois participantes por desafio, sem empates
-- ============================================================

INSERT INTO progresso_desafio (
    id,
    desafio_id,
    usuario_id,
    valor_atual,
    aceito_em,
    atualizado_em
) VALUES
(
    '91000000-0000-0000-0000-000000000001',
    '90000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    185,
    now() - INTERVAL '5 days',
    now() - INTERVAL '2 hours'
),
(
    '91000000-0000-0000-0000-000000000002',
    '90000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002',
    140,
    now() - INTERVAL '5 days',
    now() - INTERVAL '4 hours'
),

-- Finalizado: usuário fixo GANHOU
(
    '91000000-0000-0000-0000-000000000003',
    '90000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000003',
    245,
    now() - INTERVAL '30 days',
    now() - INTERVAL '16 days'
),
(
    '91000000-0000-0000-0000-000000000004',
    '90000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    320,
    now() - INTERVAL '30 days',
    now() - INTERVAL '16 days'
),

-- Finalizado: usuário fixo PERDEU
(
    '91000000-0000-0000-0000-000000000005',
    '90000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000001',
    0,
    now() - INTERVAL '50 days',
    now() - INTERVAL '40 days'
),
(
    '91000000-0000-0000-0000-000000000006',
    '90000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000004',
    1,
    now() - INTERVAL '50 days',
    now() - INTERVAL '40 days'
),

-- Pendente
(
    '91000000-0000-0000-0000-000000000007',
    '90000000-0000-0000-0000-000000000004',
    '00000000-0000-0000-0000-000000000005',
    10,
    now(),
    now()
),
(
    '91000000-0000-0000-0000-000000000008',
    '90000000-0000-0000-0000-000000000004',
    '00000000-0000-0000-0000-000000000001',
    0,
    now(),
    now()
);


-- ============================================================
-- 21. CONQUISTAS
-- 6 conquistas
-- ============================================================

INSERT INTO conquista (
    id,
    nome,
    descricao,
    icone_url,
    criterio,
    meta_valor,
    recompensa_xp
) VALUES
(
    'a0000000-0000-0000-0000-000000000001',
    'Primeiros Passos',
    'Complete sua primeira leitura.',
    'https://placehold.co/128x128?text=1',
    'livros_lidos',
    1,
    50
),
(
    'a0000000-0000-0000-0000-000000000002',
    'Leitor Dedicado',
    'Complete cinco livros.',
    'https://placehold.co/128x128?text=5',
    'livros_lidos',
    5,
    150
),
(
    'a0000000-0000-0000-0000-000000000003',
    'Mil Páginas',
    'Leia um total de mil páginas.',
    'https://placehold.co/128x128?text=1000',
    'paginas_lidas',
    1000,
    200
),
(
    'a0000000-0000-0000-0000-000000000004',
    'Em Boa Companhia',
    'Participe de um clube do livro.',
    'https://placehold.co/128x128?text=Clube',
    'clubes',
    1,
    75
),
(
    'a0000000-0000-0000-0000-000000000005',
    'Competidor',
    'Conclua seu primeiro desafio entre amigos.',
    'https://placehold.co/128x128?text=Desafio',
    'desafios',
    1,
    100
),
(
    'a0000000-0000-0000-0000-000000000006',
    'Lenda da Leitura',
    'Alcance dez mil páginas lidas.',
    'https://placehold.co/128x128?text=Lenda',
    'paginas_lidas',
    10000,
    500
);


-- ============================================================
-- 22. CONQUISTAS DO USUÁRIO FIXO
--
-- 5 desbloqueadas.
-- "Lenda da Leitura" fica bloqueada porque não há registro
-- correspondente em conquista_usuario.
-- ============================================================

INSERT INTO conquista_usuario (
    id,
    conquista_id,
    usuario_id,
    obtida_em
) VALUES
(
    'a1000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    now() - INTERVAL '150 days'
),
(
    'a1000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    now() - INTERVAL '100 days'
),
(
    'a1000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000001',
    now() - INTERVAL '70 days'
),
(
    'a1000000-0000-0000-0000-000000000004',
    'a0000000-0000-0000-0000-000000000004',
    '00000000-0000-0000-0000-000000000001',
    now() - INTERVAL '50 days'
),
(
    'a1000000-0000-0000-0000-000000000005',
    'a0000000-0000-0000-0000-000000000005',
    '00000000-0000-0000-0000-000000000001',
    now() - INTERVAL '15 days'
);