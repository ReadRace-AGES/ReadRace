-- Os e-mails do seed usavam @readrace.com, um domínio real registrado por terceiros e com caixa
-- de e-mail ativa. Como o login vincula conta nova a usuário existente pelo e-mail verificado,
-- o dono desse domínio poderia assumir os perfis do seed, inclusive o de demonstração.
-- example.com é reservado para exemplos (RFC 2606): ninguém pode ser dono dele.
UPDATE usuario
SET email = replace(email, '@readrace.com', '@example.com')
WHERE email LIKE '%@readrace.com';
