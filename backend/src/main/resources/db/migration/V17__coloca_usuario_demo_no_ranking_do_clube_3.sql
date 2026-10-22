-- V17 - Usuário de demonstração no ranking do clube do quiz
--
-- Reservada para a #200. Na V13 o usuário de demonstração entrou no clube 3, "Jornada pela
-- Fantasia", com 0 pontos, e os outros 8 membros têm entre 485 e 1066. Como a Página do clube
-- mostra só as 7 primeiras posições, ele ficava fora do ranking e a subida pelo quiz não se via.
--
-- Decisão do gestor em 16/10: ele começa em 5º, com 790 pontos, entre o 4º (817) e o 6º (734).
-- Um acerto no quiz (+50) leva a 840 e o coloca em 4º na frente de quem assiste.
--
-- Só atualiza a linha que já existe: não cria nem remove membro, não mexe no quiz e não toca nos
-- clubes 1 e 2, onde ele é o administrador e já é o 1º.

UPDATE membro_clube
SET pontos = 790
WHERE clube_id = '50000000-0000-0000-0000-000000000003'
  AND usuario_id = '00000000-0000-0000-0000-000000000001';
