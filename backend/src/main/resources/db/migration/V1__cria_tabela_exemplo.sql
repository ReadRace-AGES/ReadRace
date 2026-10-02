-- =============================================================
-- V1 - Tabela de exemplo
--
-- Convenções de migration:
--   * Nome do arquivo: V<numero>__descricao_em_snake_case.sql
--   * O numero NUNCA se repete. Confira o maior numero da pasta
--     antes de criar o seu.
--   * Migration que ja foi para a dev é IMUTAVEL. Precisa corrigir?
--     Crie uma nova. O Flyway guarda um checksum e a aplicacao se
--     recusa a subir se voce editar uma antiga.
--   * Tabela no singular, coluna em snake_case.
-- =============================================================

CREATE TABLE exemplo (
                         id        BIGSERIAL PRIMARY KEY,
                         nome      VARCHAR(100) NOT NULL,
                         descricao VARCHAR(255)
);