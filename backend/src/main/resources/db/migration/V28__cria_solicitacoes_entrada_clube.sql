CREATE TYPE situacao_solicitacao_clube AS ENUM ('pendente', 'aceita', 'recusada');

CREATE TABLE solicitacao_entrada_clube (
    id UUID NOT NULL,
    clube_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    situacao situacao_solicitacao_clube NOT NULL,
    criada_em TIMESTAMPTZ NOT NULL,
    respondida_em TIMESTAMPTZ,
    administrador_id UUID,

    CONSTRAINT pk_solicitacao_entrada_clube PRIMARY KEY (id),
    CONSTRAINT fk_solicitacao_entrada_clube_clube
        FOREIGN KEY (clube_id) REFERENCES clube_do_livro (id),
    CONSTRAINT fk_solicitacao_entrada_clube_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_solicitacao_entrada_clube_administrador
        FOREIGN KEY (administrador_id) REFERENCES usuario (id),
    CONSTRAINT chk_solicitacao_entrada_clube_resposta CHECK (
        (situacao = 'pendente' AND respondida_em IS NULL AND administrador_id IS NULL)
        OR (situacao IN ('aceita', 'recusada')
            AND respondida_em IS NOT NULL AND administrador_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_solicitacao_entrada_clube_pendente
    ON solicitacao_entrada_clube (clube_id, usuario_id)
    WHERE situacao = 'pendente';

CREATE INDEX idx_solicitacao_entrada_clube_pendentes_criacao
    ON solicitacao_entrada_clube (clube_id, criada_em, id)
    WHERE situacao = 'pendente';

-- Pedro e Julia não participam dos clubes 1 e 2; Daniel administra ambos.
INSERT INTO solicitacao_entrada_clube (id, clube_id, usuario_id, situacao, criada_em)
VALUES
    ('78000000-0000-0000-0000-000000000001',
     '50000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000009', 'pendente', now() - interval '4 days'),
    ('78000000-0000-0000-0000-000000000002',
     '50000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000010', 'pendente', now() - interval '3 days'),
    ('78000000-0000-0000-0000-000000000003',
     '50000000-0000-0000-0000-000000000002',
     '00000000-0000-0000-0000-000000000009', 'pendente', now() - interval '2 days'),
    ('78000000-0000-0000-0000-000000000004',
     '50000000-0000-0000-0000-000000000002',
     '00000000-0000-0000-0000-000000000010', 'pendente', now() - interval '1 day');
