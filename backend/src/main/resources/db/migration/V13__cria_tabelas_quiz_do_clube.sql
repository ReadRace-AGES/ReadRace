-- V13 - Tabelas do quiz do clube
--
-- Reservada para a #160. Completa a tabela `quiz` (hoje só tem `id`) e cria as quatro tabelas
-- que #161 (configurar) e #162 (responder) vão usar. Também prepara o seed: o usuário de
-- demonstração é líder dos clubes 1 e 2, então vira membro comum de um terceiro clube (o 3,
-- "Jornada pela Fantasia") com um quiz ativo já pronto para testar a tela de responder sem
-- esperar a tela de configurar.

ALTER TABLE quiz
    ADD COLUMN clube_id UUID NOT NULL,
    ADD COLUMN livro_id UUID NOT NULL,
    ADD COLUMN criado_por UUID NOT NULL,
    ADD COLUMN titulo VARCHAR(150) NOT NULL,
    ADD COLUMN recompensa_xp INTEGER NOT NULL,
    ADD COLUMN criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE quiz
    ADD CONSTRAINT fk_quiz_clube
        FOREIGN KEY (clube_id) REFERENCES clube_do_livro (id)
        ON DELETE NO ACTION ON UPDATE NO ACTION,
    ADD CONSTRAINT fk_quiz_livro
        FOREIGN KEY (livro_id) REFERENCES livro (id)
        ON DELETE NO ACTION ON UPDATE NO ACTION,
    ADD CONSTRAINT fk_quiz_criado_por
        FOREIGN KEY (criado_por) REFERENCES usuario (id)
        ON DELETE NO ACTION ON UPDATE NO ACTION,
    ADD CONSTRAINT chk_quiz_recompensa_xp
        CHECK (recompensa_xp BETWEEN 10 AND 500);

-- Um quiz ativo por clube. Criar um novo desativa o anterior (regra de #161); aqui só a restrição
-- que impede dois ativos ao mesmo tempo. Quizzes inativos não são apagados e podem se acumular.
CREATE UNIQUE INDEX uq_quiz_clube_ativo
    ON quiz (clube_id)
    WHERE ativo;

CREATE TABLE pergunta_quiz (
    id UUID NOT NULL,
    quiz_id UUID NOT NULL,
    enunciado TEXT NOT NULL,
    ordem SMALLINT NOT NULL,

    CONSTRAINT pk_pergunta_quiz PRIMARY KEY (id),

    CONSTRAINT uq_pergunta_quiz_ordem
        UNIQUE (quiz_id, ordem),

    CONSTRAINT chk_pergunta_quiz_ordem
        CHECK (ordem >= 1),

    CONSTRAINT fk_pergunta_quiz_quiz
        FOREIGN KEY (quiz_id)
        REFERENCES quiz (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE TABLE alternativa_quiz (
    id UUID NOT NULL,
    pergunta_id UUID NOT NULL,
    letra CHAR(1) NOT NULL,
    texto TEXT NOT NULL,
    correta BOOLEAN NOT NULL DEFAULT false,

    CONSTRAINT pk_alternativa_quiz PRIMARY KEY (id),

    CONSTRAINT uq_alternativa_quiz_letra
        UNIQUE (pergunta_id, letra),

    CONSTRAINT chk_alternativa_quiz_letra
        CHECK (letra IN ('A', 'B', 'C', 'D')),

    CONSTRAINT fk_alternativa_quiz_pergunta
        FOREIGN KEY (pergunta_id)
        REFERENCES pergunta_quiz (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

-- Cada pergunta tem exatamente uma alternativa correta. O banco não consegue garantir que sejam
-- exatamente 4 (restrição entre linhas de contagem), então isso fica a cargo de quem cria o quiz.
CREATE UNIQUE INDEX uq_alternativa_quiz_correta
    ON alternativa_quiz (pergunta_id)
    WHERE correta;

-- Lado referenciável da FK composta de resposta_quiz: garante que (pergunta_id, id) identifica
-- a alternativa de forma única, para impedir responder uma pergunta com a alternativa de outra.
CREATE UNIQUE INDEX uq_alternativa_quiz_pergunta_id
    ON alternativa_quiz (pergunta_id, id);

CREATE TABLE resposta_quiz (
    id UUID NOT NULL,
    pergunta_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    alternativa_id UUID NOT NULL,
    correta BOOLEAN NOT NULL,
    respondida_em TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_resposta_quiz PRIMARY KEY (id),

    -- Um usuário responde uma pergunta uma vez só.
    CONSTRAINT uq_resposta_quiz_usuario
        UNIQUE (pergunta_id, usuario_id),

    CONSTRAINT fk_resposta_quiz_pergunta
        FOREIGN KEY (pergunta_id)
        REFERENCES pergunta_quiz (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_resposta_quiz_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    -- Composta de propósito: amarra a alternativa à mesma pergunta da resposta, para que uma
    -- resposta não possa apontar para a alternativa de outra pergunta do mesmo quiz.
    CONSTRAINT fk_resposta_quiz_alternativa
        FOREIGN KEY (pergunta_id, alternativa_id)
        REFERENCES alternativa_quiz (pergunta_id, id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE TABLE conclusao_quiz (
    id UUID NOT NULL,
    quiz_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    concluida_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    xp_pago INTEGER NOT NULL,

    CONSTRAINT pk_conclusao_quiz PRIMARY KEY (id),

    -- Uma conclusão por usuário por quiz: é o que impede pagar o XP duas vezes.
    CONSTRAINT uq_conclusao_quiz_usuario
        UNIQUE (quiz_id, usuario_id),

    CONSTRAINT chk_conclusao_quiz_xp_pago
        CHECK (xp_pago >= 0),

    CONSTRAINT fk_conclusao_quiz_quiz
        FOREIGN KEY (quiz_id)
        REFERENCES quiz (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_conclusao_quiz_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

-- ============================================================
-- Seed: usuário de demonstração (...0001) é líder dos clubes 1 e 2.
-- Pela regra do botão "Acessar Quiz" ele nunca testaria a tela de responder, então
-- entra como membro comum do clube 3 ("Jornada pela Fantasia", livro atual: O Hobbit),
-- que já tem um quiz ativo com 3 perguntas de 4 alternativas.
-- ============================================================

INSERT INTO membro_clube (
    id,
    clube_id,
    usuario_id,
    pontos,
    entrou_em,
    cargo_clube
) VALUES (
    '74000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000001',
    0,
    now(),
    'membro'::cargo
);

-- Quiz criado pelo líder do clube 3 (usuário ...0002, administrador do clube 3 no seed V4).
INSERT INTO quiz (
    id,
    clube_id,
    livro_id,
    criado_por,
    titulo,
    recompensa_xp,
    ativo
) VALUES (
    '71000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000003',
    '30000000-0000-0000-0000-000000000008',
    '00000000-0000-0000-0000-000000000002',
    'Quiz sobre O Hobbit',
    100,
    true
);

INSERT INTO pergunta_quiz (id, quiz_id, enunciado, ordem) VALUES
('72000000-0000-0000-0000-000000000001', '71000000-0000-0000-0000-000000000001',
 'Quem é o autor de "O Hobbit"?', 1),
('72000000-0000-0000-0000-000000000002', '71000000-0000-0000-0000-000000000001',
 'Qual é o nome do protagonista de "O Hobbit"?', 2),
('72000000-0000-0000-0000-000000000003', '71000000-0000-0000-0000-000000000001',
 'Que criatura guarda o tesouro na Montanha Solitária em "O Hobbit"?', 3);

INSERT INTO alternativa_quiz (id, pergunta_id, letra, texto, correta) VALUES
('73000000-0000-0000-0000-000000000001', '72000000-0000-0000-0000-000000000001',
 'A', 'J. R. R. Tolkien', true),
('73000000-0000-0000-0000-000000000002', '72000000-0000-0000-0000-000000000001',
 'B', 'George Orwell', false),
('73000000-0000-0000-0000-000000000003', '72000000-0000-0000-0000-000000000001',
 'C', 'C. S. Lewis', false),
('73000000-0000-0000-0000-000000000004', '72000000-0000-0000-0000-000000000001',
 'D', 'Mary Shelley', false),
('73000000-0000-0000-0000-000000000005', '72000000-0000-0000-0000-000000000002',
 'A', 'Frodo Bolseiro', false),
('73000000-0000-0000-0000-000000000006', '72000000-0000-0000-0000-000000000002',
 'B', 'Bilbo Bolseiro', true),
('73000000-0000-0000-0000-000000000007', '72000000-0000-0000-0000-000000000002',
 'C', 'Aragorn', false),
('73000000-0000-0000-0000-000000000008', '72000000-0000-0000-0000-000000000002',
 'D', 'Samwise Gamgee', false),
('73000000-0000-0000-0000-000000000009', '72000000-0000-0000-0000-000000000003',
 'A', 'Um dragão chamado Smaug', true),
('73000000-0000-0000-0000-000000000010', '72000000-0000-0000-0000-000000000003',
 'B', 'Um troll de pedra', false),
('73000000-0000-0000-0000-000000000011', '72000000-0000-0000-0000-000000000003',
 'C', 'Uma aranha gigante', false),
('73000000-0000-0000-0000-000000000012', '72000000-0000-0000-0000-000000000003',
 'D', 'Um orc chamado Azog', false);
