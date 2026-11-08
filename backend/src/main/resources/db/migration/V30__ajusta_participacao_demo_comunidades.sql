-- #235: o demo mantém as comunidades 1, 2 e 3; as comunidades 4 e 5 ficam como sugestões.
-- No seed V4, Mariana e Gabriel administram respectivamente as comunidades 4 e 5.
-- Comentários e respostas não têm comunidade_id: o destino vem da raiz da thread (V19).
-- Só autor_id é atualizado; IDs, conteúdo, datas, pais e curtidas são preservados.
DO $$
DECLARE
    destino RECORD;
    demo CONSTANT UUID := '00000000-0000-0000-0000-000000000001';
BEGIN
    FOR destino IN
        SELECT * FROM (VALUES
            ('60000000-0000-0000-0000-000000000004'::uuid,
             '00000000-0000-0000-0000-000000000004'::uuid),
            ('60000000-0000-0000-0000-000000000005'::uuid,
             '00000000-0000-0000-0000-000000000005'::uuid)
        ) AS d (comunidade_id, autor_id)
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM membro_comunidade mc
            JOIN usuario u ON u.id = mc.usuario_id
            WHERE mc.comunidade_id = destino.comunidade_id
              AND mc.usuario_id = destino.autor_id
              AND mc.cargo_comunidade = 'administrador'
              AND u.excluido_em IS NULL
        ) THEN
            RAISE EXCEPTION 'V30: administrador ativo e participante ausente na comunidade %',
                destino.comunidade_id;
        END IF;

        WITH RECURSIVE thread AS (
            SELECT id FROM post
            WHERE comunidade_id = destino.comunidade_id AND post_pai_id IS NULL
            UNION
            SELECT p.id FROM post p JOIN thread t ON p.post_pai_id = t.id
        )
        UPDATE post SET autor_id = destino.autor_id
        WHERE autor_id = demo AND id IN (SELECT id FROM thread);

        DELETE FROM membro_comunidade
        WHERE comunidade_id = destino.comunidade_id AND usuario_id = demo;
    END LOOP;
END
$$;
