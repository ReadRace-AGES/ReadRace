CREATE TYPE desafio_sistema AS ENUM ('MENSAL', 'SEMANAL');

CREATE TABLE pagamento_desafio_sistema (
    id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    desafio desafio_sistema NOT NULL,
    inicio_periodo DATE NOT NULL,
    xp_pago INTEGER NOT NULL,
    pago_em TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_pagamento_desafio_sistema PRIMARY KEY (id),
    CONSTRAINT uq_pagamento_desafio_sistema
        UNIQUE (usuario_id, desafio, inicio_periodo),
    CONSTRAINT chk_pagamento_desafio_sistema_xp_pago CHECK (xp_pago > 0),
    CONSTRAINT fk_pagamento_desafio_sistema_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

-- NULL preserva a ausência de medição nos registros históricos.
ALTER TABLE registro_leitura
    ADD COLUMN paginas_novas INTEGER,
    ADD CONSTRAINT chk_registro_leitura_paginas_novas CHECK (paginas_novas >= 0);
