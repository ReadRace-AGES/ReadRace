-- V15 - Mascotes desbloqueados pelo nível
--
-- Reservada para a #164. Completa a tabela `mascote` (hoje só tem `id`) com os 5 mascotes que têm
-- arte no frame Mascotes do Figma e guarda em `usuario` o mascote escolhido.
--
-- `imagem` é a chave da arte, não uma URL: as 5 artes vão empacotadas no app
-- (mobile/assets/images/mascotes/<imagem>.png), que traduz a chave para o arquivo.
--
-- O desbloqueio é pelo nível (decisão do gestor em 01/10): 1, 3, 5, 7 e 9, na ordem da tela.
-- `usuario.mascote_id` vazio significa o padrão, o mascote do nível 1.

ALTER TABLE mascote
    ADD COLUMN nome VARCHAR(60) NOT NULL,
    ADD COLUMN imagem VARCHAR(60) NOT NULL,
    ADD COLUMN nivel_minimo INTEGER NOT NULL,
    ADD COLUMN ordem SMALLINT NOT NULL;

ALTER TABLE mascote
    ADD CONSTRAINT uq_mascote_ordem UNIQUE (ordem),
    ADD CONSTRAINT uq_mascote_imagem UNIQUE (imagem),
    ADD CONSTRAINT chk_mascote_nivel_minimo CHECK (nivel_minimo >= 1),
    ADD CONSTRAINT chk_mascote_ordem CHECK (ordem >= 1);

INSERT INTO mascote (id, nome, imagem, nivel_minimo, ordem) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'O Bruxo', 'o-bruxo', 1, 1),
    ('b0000000-0000-0000-0000-000000000002', 'O Cinzento', 'o-cinzento', 3, 2),
    ('b0000000-0000-0000-0000-000000000003', 'O Sonhador', 'o-sonhador', 5, 3),
    ('b0000000-0000-0000-0000-000000000004', 'A Bruxa Má', 'a-bruxa-ma', 7, 4),
    ('b0000000-0000-0000-0000-000000000005', 'A Bruxa Boa', 'a-bruxa-boa', 9, 5);

ALTER TABLE usuario
    ADD COLUMN mascote_id UUID;

ALTER TABLE usuario
    ADD CONSTRAINT fk_usuario_mascote
        FOREIGN KEY (mascote_id) REFERENCES mascote (id)
        ON DELETE NO ACTION ON UPDATE NO ACTION;
