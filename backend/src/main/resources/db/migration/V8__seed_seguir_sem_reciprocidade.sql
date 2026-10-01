INSERT INTO seguir (
    id,
    seguidor_id,
    seguido_id,
    criado_em
) VALUES (
    '80000000-0000-0000-0000-000000000011',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000006',
    now() - INTERVAL '10 days'
);
