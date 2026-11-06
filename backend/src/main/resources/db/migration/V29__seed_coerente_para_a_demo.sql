-- V29 - Seed coerente para a demo (#220)
--
-- Reservada para a #220 como V25 e renomeada para V29: a V28 (#253) entrou antes na dev, e uma
-- versão menor que a última aplicada impede a API de subir. A banca navega pelos perfis do ranking
-- e pelos fóruns dos clubes, e o seed tinha incoerências visíveis. Esta migration só mexe em dados
-- do seed; não muda schema, xp_total, nivel, pontos do ranking nem registros de leitura.
--
-- 1. Bibliotecas dos outros 9 usuários coerentes com o XP deles
-- 2. Posts do usuário de demonstração nos clubes 4 e 5, onde ele não é membro
-- 3. Capa de "A Metamorfose"
-- 4. Placar do desafio de livro finalizado
-- 5. Curtidas do autor no próprio post
-- 6. Twemoji em versão fixa


-- 1. Bibliotecas coerentes com o XP
--
-- No app o XP vem de 1 por página, 150 por livro concluído, da recompensa de cada conquista e do
-- quiz. Para cada um dos outros 9 usuários, sem mudar o xp_total, vale ao fim:
--
--   xp_total = soma de pagina_maxima + 150 x livros lidos + recompensas das conquistas + XP de quiz
--
-- Cada um ganha os livros lidos do plano abaixo e um livro em andamento cujas páginas fecham a
-- conta. As conquistas concedidas são as que os números alcançam, pelos critérios do
-- ConquistaRepository. A conta usa o que já existe no banco (itens, conquistas e quiz), então
-- fecha também se outra migration de seed tiver dado leitura a essas pessoas antes (V23, #197).
--
-- O livro em andamento é O Hobbit para quem é do clube 3, o clube da demo do quiz: o perfil aberto
-- pelo ranking mostra a pessoa lendo o livro do clube. Quem não comporta nenhum livro lido (o
-- menor custa 88 + 150 + 50 de Primeiros Passos) fica só com o livro em andamento.
--
-- Se a conta de alguém não fechar (na produção o estado pode ser outro), a pessoa é pulada com um
-- aviso: uma migration com erro impediria a API de subir.
DO $$
DECLARE
    plano RECORD;
    v_usuario UUID;
    v_xp INTEGER;
    v_novos UUID[];
    v_paginas BIGINT;
    v_lidos BIGINT;
    v_clubes BIGINT;
    v_desafios BIGINT;
    v_quiz BIGINT;
    v_recompensa_fixa BIGINT;
    v_base BIGINT;
    v_por_paginas INTEGER[];
    v_extra BIGINT;
    v_candidato BIGINT;
    v_resto BIGINT;
    v_livro_em_andamento UUID;
    v_total BIGINT;
    v_dias INTEGER;
    v_livro UUID;
BEGIN
    FOR plano IN
        SELECT * FROM (VALUES
            (2, ARRAY[1, 2, 12]),   -- Ana: Dom Casmurro, 1984, Ensaio sobre a Cegueira
            (3, ARRAY[9, 4, 1]),    -- Lucas: O Pequeno Príncipe, A Metamorfose, Dom Casmurro
            (4, ARRAY[7, 18]),      -- Mariana: A Hora da Estrela, A Revolução dos Bichos
            (5, ARRAY[20]),         -- Gabriel: A Paixão Segundo G.H.
            (6, ARRAY[2]),          -- Beatriz: 1984, o mesmo livro que a V23 dá a ela
            (7, ARRAY[7]),          -- Rafael: A Hora da Estrela
            (8, ARRAY[]::INTEGER[]),
            (9, ARRAY[]::INTEGER[]),
            (10, ARRAY[]::INTEGER[])
        ) AS p (usuario, livros)
    LOOP
        v_usuario := ('00000000-0000-0000-0000-' || lpad(plano.usuario::text, 12, '0'))::uuid;

        SELECT xp_total INTO v_xp FROM usuario WHERE id = v_usuario AND excluido_em IS NULL;
        IF v_xp IS NULL THEN
            CONTINUE;
        END IF;

        -- Livros do plano que a pessoa ainda não tem.
        SELECT coalesce(array_agg(l.id ORDER BY ordem), ARRAY[]::UUID[]),
               coalesce(sum(l.total_paginas), 0)
        INTO v_novos, v_paginas
        FROM unnest(plano.livros) WITH ORDINALITY AS n (livro, ordem)
        JOIN livro l ON l.id = ('30000000-0000-0000-0000-' || lpad(n.livro::text, 12, '0'))::uuid
        WHERE NOT EXISTS (SELECT 1 FROM item_biblioteca i
                          WHERE i.usuario_id = v_usuario AND i.livro_id = l.id);

        SELECT v_paginas + coalesce(sum(pagina_maxima), 0),
               cardinality(v_novos) + count(*) FILTER (WHERE status_leitura = 'lido')
        INTO v_paginas, v_lidos
        FROM item_biblioteca WHERE usuario_id = v_usuario;

        SELECT count(*) INTO v_clubes
        FROM membro_clube m JOIN clube_do_livro c ON c.id = m.clube_id
        WHERE m.usuario_id = v_usuario AND c.excluido_em IS NULL;

        SELECT count(*) INTO v_desafios
        FROM desafio_amigo
        WHERE status = 'finalizado' AND v_usuario IN (criador_id, oponente_id);

        SELECT coalesce(sum(xp_pago), 0) INTO v_quiz
        FROM conclusao_quiz WHERE usuario_id = v_usuario;

        -- Recompensas que não dependem do livro em andamento: as conquistas que a pessoa já tem
        -- e as alcançadas por livros lidos, clubes e desafios.
        SELECT coalesce(sum(c.recompensa_xp), 0) INTO v_recompensa_fixa
        FROM conquista c
        WHERE EXISTS (SELECT 1 FROM conquista_usuario cu
                      WHERE cu.usuario_id = v_usuario AND cu.conquista_id = c.id)
           OR (c.criterio = 'livros_lidos' AND v_lidos >= c.meta_valor)
           OR (c.criterio = 'clubes' AND v_clubes >= c.meta_valor)
           OR (c.criterio = 'desafios' AND v_desafios >= c.meta_valor);

        v_base := v_xp - v_quiz - v_paginas - 150 * v_lidos - v_recompensa_fixa;

        -- As conquistas por páginas dependem das páginas do livro em andamento, que dependem das
        -- recompensas. Tenta 0, 1, 2... conquistas por páginas ainda não obtidas, em ordem de meta,
        -- e fica com a primeira tentativa em que o total de páginas alcança exatamente essas.
        SELECT coalesce(array_agg(c.recompensa_xp ORDER BY c.meta_valor), ARRAY[]::INTEGER[])
        INTO v_por_paginas
        FROM conquista c
        WHERE c.criterio = 'paginas_lidas'
          AND NOT EXISTS (SELECT 1 FROM conquista_usuario cu
                          WHERE cu.usuario_id = v_usuario AND cu.conquista_id = c.id);

        v_resto := NULL;
        v_extra := 0;
        FOR k IN 0 .. cardinality(v_por_paginas) LOOP
            IF k > 0 THEN
                v_extra := v_extra + v_por_paginas[k];
            END IF;
            v_candidato := v_base - v_extra;
            IF v_candidato >= 0
               AND (SELECT count(*) FROM conquista c
                    WHERE c.criterio = 'paginas_lidas'
                      AND c.meta_valor <= v_paginas + v_candidato
                      AND NOT EXISTS (SELECT 1 FROM conquista_usuario cu
                                      WHERE cu.usuario_id = v_usuario
                                        AND cu.conquista_id = c.id)) = k THEN
                v_resto := v_candidato;
                EXIT;
            END IF;
        END LOOP;

        IF v_resto IS NULL THEN
            RAISE NOTICE 'V29: o XP de % (%) não fecha com a biblioteca; usuário pulado.',
                v_usuario, v_xp;
            CONTINUE;
        END IF;

        -- O livro em andamento precisa ter mais páginas que o resto, senão estaria lido.
        v_livro_em_andamento := NULL;
        IF v_resto > 0 THEN
            SELECT l.id INTO v_livro_em_andamento
            FROM livro l
            WHERE l.total_paginas > v_resto
              AND l.id <> ALL (v_novos)
              AND NOT EXISTS (SELECT 1 FROM item_biblioteca i
                              WHERE i.usuario_id = v_usuario AND i.livro_id = l.id)
            ORDER BY
                -- O Hobbit, para quem é do clube 3
                (l.id = '30000000-0000-0000-0000-000000000008'
                 AND EXISTS (SELECT 1 FROM membro_clube m
                             WHERE m.usuario_id = v_usuario
                               AND m.clube_id = '50000000-0000-0000-0000-000000000003')) DESC,
                -- depois o livro de outro clube da pessoa
                EXISTS (SELECT 1 FROM membro_clube m JOIN clube_do_livro c ON c.id = m.clube_id
                        WHERE m.usuario_id = v_usuario AND c.livro_id = l.id) DESC,
                l.total_paginas,
                l.id
            LIMIT 1;

            IF v_livro_em_andamento IS NULL THEN
                RAISE NOTICE 'V29: nenhum livro com mais de % páginas para %; usuário pulado.',
                    v_resto, v_usuario;
                CONTINUE;
            END IF;
        END IF;

        v_dias := 60;
        FOREACH v_livro IN ARRAY v_novos LOOP
            INSERT INTO item_biblioteca (id, usuario_id, livro_id, status_leitura, pagina_atual,
                                         pagina_maxima, adicionado_em)
            SELECT md5('v25-item-' || v_usuario || '-' || l.id)::uuid, v_usuario, l.id, 'lido',
                   l.total_paginas, l.total_paginas, now() - make_interval(days => v_dias)
            FROM livro l WHERE l.id = v_livro
            ON CONFLICT (usuario_id, livro_id) DO NOTHING;
            v_dias := v_dias - 15;
        END LOOP;

        IF v_livro_em_andamento IS NOT NULL THEN
            INSERT INTO item_biblioteca (id, usuario_id, livro_id, status_leitura, pagina_atual,
                                         pagina_maxima, adicionado_em)
            VALUES (md5('v25-item-' || v_usuario || '-' || v_livro_em_andamento)::uuid, v_usuario,
                    v_livro_em_andamento, 'lendo', v_resto, v_resto, now() - INTERVAL '10 days')
            ON CONFLICT (usuario_id, livro_id) DO NOTHING;
        END IF;

        v_total := v_paginas + v_resto;
        INSERT INTO conquista_usuario (id, conquista_id, usuario_id, obtida_em)
        SELECT md5('v25-conquista-' || v_usuario || '-' || c.id)::uuid, c.id, v_usuario,
               now() - INTERVAL '5 days'
        FROM conquista c
        WHERE (c.criterio = 'livros_lidos' AND v_lidos >= c.meta_valor)
           OR (c.criterio = 'paginas_lidas' AND v_total >= c.meta_valor)
           OR (c.criterio = 'clubes' AND v_clubes >= c.meta_valor)
           OR (c.criterio = 'desafios' AND v_desafios >= c.meta_valor)
        ON CONFLICT (usuario_id, conquista_id) DO NOTHING;
    END LOOP;
END
$$;


-- 2. Os posts do usuário de demonstração nos fóruns dos clubes 4 e 5 (V4, seção 14) passam para
--    o Pedro, membro dos dois clubes e que ainda não posta neles.
UPDATE post
SET autor_id = '00000000-0000-0000-0000-000000000009'
WHERE id IN (md5('post-clube-4-6')::uuid, md5('post-clube-5-5')::uuid)
  AND autor_id = '00000000-0000-0000-0000-000000000001';


-- 3. A capa de "A Metamorfose" era a de "O Processo" (ISBN 9780805209990, "The Trial", Schocken).
--    9788571646858 é "A Metamorfose" da Companhia das Letras, tradução de Modesto Carone.
UPDATE livro
SET capa_url = 'https://covers.openlibrary.org/b/isbn/9788571646858-L.jpg'
WHERE id = '30000000-0000-0000-0000-000000000004'
  AND capa_url = 'https://covers.openlibrary.org/b/isbn/9780805209990-L.jpg';


-- 4. No desafio de livro o app grava as páginas do livro como placar. O desafio de A Metamorfose
--    (128 páginas) já finalizado tinha 0 x 1; a Mariana venceu terminando o livro.
UPDATE progresso_desafio AS pd
SET valor_atual = s.valor
FROM (VALUES ('91000000-0000-0000-0000-000000000005'::uuid, 90),
             ('91000000-0000-0000-0000-000000000006'::uuid, 128)) AS s (id, valor)
WHERE pd.id = s.id
  AND pd.desafio_id = '90000000-0000-0000-0000-000000000003';


-- 5. Curtidas do seed (V4, seção 17) em que o autor curte o próprio post: eram 35, de 6
--    usuários. Só as do seed, pelos ids: o app não impede curtir o próprio post, e o que a conta
--    demo fizer pelo app não é dado do seed.
DELETE FROM curtida AS c
USING post AS p
WHERE p.id = c.post_id
  AND c.usuario_id = p.autor_id
  AND c.id IN (
      SELECT md5('curtida-clube-' || cl || '-' || po || '-' || u)::uuid
      FROM generate_series(1, 5) AS cl, generate_series(1, 6) AS po,
           generate_series(1, 10) AS u
      UNION ALL
      SELECT md5('curtida-comunidade-' || co || '-' || po || '-' || u)::uuid
      FROM generate_series(1, 5) AS co, generate_series(1, 6) AS po,
           generate_series(1, 10) AS u);


-- 6. Ícones das conquistas no Twemoji em versão fixa (17.0.3, a mais recente no jsDelivr em
--    04/11/2026), em vez de @latest, que pode mudar o desenho sem aviso.
UPDATE conquista
SET icone_url = replace(icone_url, '/twemoji@latest/', '/twemoji@17.0.3/')
WHERE icone_url LIKE '%/twemoji@latest/%';
