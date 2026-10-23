-- V20 - Plano de fundo do perfil
--
-- Guarda a chave do plano de fundo escolhido nas Configurações de perfil. Como no mascote, é a
-- chave da arte, não uma URL: os fundos são desenhados no app, que traduz a chave para a arte.
-- Vazio significa o padrão, o cenário que o perfil já mostrava.
--
-- O avatar escolhido entre os bonecos usa a coluna `avatar_url`, que já existe: a chave do boneco
-- ocupa o lugar da URL da foto, e o app desenha o boneco onde quer que o avatar apareça.

ALTER TABLE usuario
    ADD COLUMN plano_fundo VARCHAR(30);
