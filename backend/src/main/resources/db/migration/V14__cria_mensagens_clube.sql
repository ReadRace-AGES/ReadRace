-- V14 - Chat dos clubes
-- Task #151: Conversar com os membros do clube no chat do clube

CREATE TABLE mensagem_clube (
    id UUID NOT NULL,
    clube_id UUID NOT NULL,
    autor_id UUID NOT NULL,
    texto VARCHAR(1000) NOT NULL,
    enviada_em TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_mensagem_clube
        PRIMARY KEY (id),

    CONSTRAINT fk_mensagem_clube_clube
        FOREIGN KEY (clube_id)
        REFERENCES clube_do_livro (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_mensagem_clube_autor
        FOREIGN KEY (autor_id)
        REFERENCES usuario (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT chk_mensagem_clube_texto
        CHECK (
            char_length(texto) BETWEEN 1 AND 1000
            AND char_length(trim(texto)) > 0
        )
);

-- Facilita:
-- GET /api/clubes/{clubeId}/mensagens
-- e GET /api/clubes/{clubeId}/mensagens?depois=...
CREATE INDEX idx_mensagem_clube_clube_enviada
    ON mensagem_clube (clube_id, enviada_em);


-- ============================================================
-- SEED
--
-- O usuário de demonstração (...0001) participa dos clubes
-- 1 e 2 pelo seed original e do clube 3 pela migration V13.
-- As mensagens abaixo permitem testar o chat imediatamente.
-- ============================================================


-- Clube 1 - Clube dos Clássicos Brasileiros

INSERT INTO mensagem_clube (
    id,
    clube_id,
    autor_id,
    texto,
    enviada_em
) VALUES
(
    '75000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002',
    'O que vocês estão achando da leitura até agora?',
    now() - INTERVAL '3 hours'
),
(
    '75000000-0000-0000-0000-000000000002',
    '50000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'Estou gostando bastante. Quero continuar hoje.',
    now() - INTERVAL '2 hours'
),
(
    '75000000-0000-0000-0000-000000000003',
    '50000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000003',
    'Também estou! Depois podemos comentar o próximo capítulo.',
    now() - INTERVAL '1 hour'
);


-- Clube 2 - Distopias em Debate

INSERT INTO mensagem_clube (
    id,
    clube_id,
    autor_id,
    texto,
    enviada_em
) VALUES
(
    '75000000-0000-0000-0000-000000000004',
    '50000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000002',
    'Qual parte do livro chamou mais atenção de vocês?',
    now() - INTERVAL '4 hours'
),
(
    '75000000-0000-0000-0000-000000000005',
    '50000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'Para mim foi a construção do mundo.',
    now() - INTERVAL '2 hours'
);


-- Clube 3 - Jornada pela Fantasia
-- O usuário de demonstração passou a integrar este clube na V13.

INSERT INTO mensagem_clube (
    id,
    clube_id,
    autor_id,
    texto,
    enviada_em
) VALUES
(
    '75000000-0000-0000-0000-000000000006',
    '50000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000002',
    'Bem-vindos ao chat do clube!',
    now() - INTERVAL '2 hours'
),
(
    '75000000-0000-0000-0000-000000000007',
    '50000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000001',
    'Valeu! Animado para conversar sobre O Hobbit.',
    now() - INTERVAL '1 hour'
);