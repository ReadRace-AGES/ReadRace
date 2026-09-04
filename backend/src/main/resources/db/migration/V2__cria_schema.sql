CREATE TYPE cargo AS ENUM (
    'administrador',
    'membro'
);

CREATE TYPE status_leitura AS ENUM (
    'lendo',
    'lido',
    'desejo'
);

CREATE TYPE tipo_meta_desafio AS ENUM (
    'paginas',
    'livro'
);

CREATE TYPE status_desafio AS ENUM (
    'pendente',
    'ativo',
    'recusado',
    'finalizado'
);

CREATE TABLE usuario (
    id UUID NOT NULL,
    nome VARCHAR(120) NOT NULL,
    nome_usuario VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    google_id VARCHAR(255),
    avatar_url TEXT,
    xp_total INTEGER NOT NULL DEFAULT 0,
    meta_frequencia INTEGER,
    meta_paginas INTEGER,
    dias_consecutivos INTEGER NOT NULL DEFAULT 0,
    ultima_leitura_em DATE,
    nivel INTEGER NOT NULL DEFAULT 1,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    excluido_em TIMESTAMPTZ,

    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uq_usuario_nome_usuario UNIQUE (nome_usuario),
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT uq_usuario_google_id UNIQUE (google_id),
    CONSTRAINT chk_usuario_xp_total CHECK (xp_total >= 0),
    CONSTRAINT chk_usuario_meta_frequencia
        CHECK (meta_frequencia IS NULL OR meta_frequencia > 0),
    CONSTRAINT chk_usuario_meta_paginas
        CHECK (meta_paginas IS NULL OR meta_paginas > 0),
    CONSTRAINT chk_usuario_dias_consecutivos
        CHECK (dias_consecutivos >= 0),
    CONSTRAINT chk_usuario_nivel CHECK (nivel >= 1)
);

CREATE TABLE livro (
    id UUID NOT NULL,
    isbn VARCHAR(13) NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    subtitulo VARCHAR(255),
    total_paginas INTEGER NOT NULL,
    capa_url TEXT,
    data_publicacao DATE,

    CONSTRAINT pk_livro PRIMARY KEY (id),
    CONSTRAINT uq_livro_isbn UNIQUE (isbn),
    CONSTRAINT chk_livro_total_paginas CHECK (total_paginas > 0)
);

CREATE TABLE autor (
    id UUID NOT NULL,
    nome VARCHAR(255) NOT NULL,

    CONSTRAINT pk_autor PRIMARY KEY (id)
);

CREATE TABLE genero (
    id UUID NOT NULL,
    nome VARCHAR(100) NOT NULL,

    CONSTRAINT pk_genero PRIMARY KEY (id),
    CONSTRAINT uq_genero_nome UNIQUE (nome)
);

CREATE TABLE comunidade (
    id UUID NOT NULL,
    nome VARCHAR(120) NOT NULL,
    descricao TEXT,
    imagem_url TEXT,
    criada_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    excluido_em TIMESTAMPTZ,

    CONSTRAINT pk_comunidade PRIMARY KEY (id)
);

CREATE TABLE conquista (
    id UUID NOT NULL,
    nome VARCHAR(120) NOT NULL,
    descricao TEXT NOT NULL,
    icone_url TEXT,
    criterio VARCHAR(30) NOT NULL,
    meta_valor INTEGER NOT NULL,
    recompensa_xp INTEGER NOT NULL DEFAULT 0,
    criada_em TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_conquista PRIMARY KEY (id),
    CONSTRAINT chk_conquista_meta_valor CHECK (meta_valor > 0),
    CONSTRAINT chk_conquista_recompensa_xp CHECK (recompensa_xp >= 0)
);

CREATE TABLE livro_autor (
    livro_id UUID NOT NULL,
    autor_id UUID NOT NULL,
    ordem SMALLINT NOT NULL,

    CONSTRAINT pk_livro_autor
        PRIMARY KEY (livro_id, autor_id),

    CONSTRAINT uq_livro_autor_ordem
        UNIQUE (livro_id, ordem),

    CONSTRAINT chk_livro_autor_ordem
        CHECK (ordem >= 1),

    CONSTRAINT fk_livro_autor_livro
        FOREIGN KEY (livro_id)
        REFERENCES livro (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_livro_autor_autor
        FOREIGN KEY (autor_id)
        REFERENCES autor (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE TABLE livro_genero (
    livro_id UUID NOT NULL,
    genero_id UUID NOT NULL,

    CONSTRAINT pk_livro_genero
        PRIMARY KEY (livro_id, genero_id),

    CONSTRAINT fk_livro_genero_livro
        FOREIGN KEY (livro_id)
        REFERENCES livro (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_livro_genero_genero
        FOREIGN KEY (genero_id)
        REFERENCES genero (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE TABLE clube_do_livro (
    id UUID NOT NULL,
    livro_id UUID NOT NULL,
    lider_id UUID NOT NULL,
    nome VARCHAR(120) NOT NULL,
    descricao TEXT,
    capa_url TEXT,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    excluido_em TIMESTAMPTZ,

    CONSTRAINT pk_clube_do_livro PRIMARY KEY (id),

    CONSTRAINT fk_clube_do_livro_livro
        FOREIGN KEY (livro_id)
        REFERENCES livro (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_clube_do_livro_lider
        FOREIGN KEY (lider_id)
        REFERENCES usuario (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE TABLE membro_clube (
    id UUID NOT NULL,
    clube_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    pontos INTEGER NOT NULL DEFAULT 0,
    entrou_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    cargo_clube cargo NOT NULL,

    CONSTRAINT pk_membro_clube PRIMARY KEY (id),

    CONSTRAINT uq_membro_clube_usuario
        UNIQUE (clube_id, usuario_id),

    CONSTRAINT chk_membro_clube_pontos
        CHECK (pontos >= 0),

    CONSTRAINT fk_membro_clube_clube
        FOREIGN KEY (clube_id)
        REFERENCES clube_do_livro (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_membro_clube_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE TABLE meta_clube (
    id UUID NOT NULL,
    clube_id UUID NOT NULL,
    tipo_meta tipo_meta_desafio NOT NULL,
    valor_alvo INTEGER NOT NULL,
    inicio_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    fim_em TIMESTAMPTZ,

    CONSTRAINT pk_meta_clube PRIMARY KEY (id),

    CONSTRAINT chk_meta_clube_valor_alvo
        CHECK (valor_alvo > 0),

    CONSTRAINT chk_meta_clube_periodo
        CHECK (fim_em IS NULL OR fim_em > inicio_em),

    CONSTRAINT fk_meta_clube_clube
        FOREIGN KEY (clube_id)
        REFERENCES clube_do_livro (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE TABLE membro_comunidade (
    id UUID NOT NULL,
    comunidade_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    entrou_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    cargo_comunidade cargo NOT NULL,

    CONSTRAINT pk_membro_comunidade PRIMARY KEY (id),

    CONSTRAINT uq_membro_comunidade_usuario
        UNIQUE (comunidade_id, usuario_id),

    CONSTRAINT fk_membro_comunidade_comunidade
        FOREIGN KEY (comunidade_id)
        REFERENCES comunidade (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_membro_comunidade_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE TABLE item_biblioteca (
    id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    livro_id UUID NOT NULL,
    status_leitura status_leitura NOT NULL,
    favorito BOOLEAN NOT NULL DEFAULT false,
    pagina_atual INTEGER NOT NULL DEFAULT 0,
    pagina_maxima INTEGER NOT NULL DEFAULT 0,
    adicionado_em TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_item_biblioteca PRIMARY KEY (id),

    CONSTRAINT uq_item_biblioteca_usuario_livro
        UNIQUE (usuario_id, livro_id),

    CONSTRAINT chk_item_biblioteca_pagina_atual
        CHECK (pagina_atual >= 0),

    CONSTRAINT chk_item_biblioteca_pagina_maxima
        CHECK (pagina_maxima >= 0),

    CONSTRAINT chk_item_biblioteca_paginas_coerentes
        CHECK (pagina_atual <= pagina_maxima),

    CONSTRAINT fk_item_biblioteca_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_item_biblioteca_livro
        FOREIGN KEY (livro_id)
        REFERENCES livro (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE TABLE registro_leitura (
    id UUID NOT NULL,
    item_biblioteca_id UUID NOT NULL,
    ultima_pagina INTEGER NOT NULL,
    registrado_em TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_registro_leitura PRIMARY KEY (id),

    CONSTRAINT chk_registro_leitura_ultima_pagina
        CHECK (ultima_pagina >= 0),

    CONSTRAINT fk_registro_leitura_item_biblioteca
        FOREIGN KEY (item_biblioteca_id)
        REFERENCES item_biblioteca (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE INDEX idx_registro_leitura_item
    ON registro_leitura (item_biblioteca_id);

CREATE TABLE post (
    id UUID NOT NULL,
    autor_id UUID NOT NULL,
    post_pai_id UUID,
    livro_id UUID,
    clube_id UUID,
    comunidade_id UUID,
    conteudo TEXT NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    excluido_em TIMESTAMPTZ,

    CONSTRAINT pk_post PRIMARY KEY (id),

    CONSTRAINT chk_post_destino_unico
        CHECK (
            NOT (
                clube_id IS NOT NULL
                AND comunidade_id IS NOT NULL
            )
        ),

    CONSTRAINT chk_comentario_sem_escopo
        CHECK (
            post_pai_id IS NULL
            OR (
                livro_id IS NULL
                AND clube_id IS NULL
                AND comunidade_id IS NULL
            )
        ),

    CONSTRAINT chk_post_nao_pode_ser_pai_de_si
        CHECK (
            post_pai_id IS NULL
            OR post_pai_id <> id
        ),

    CONSTRAINT fk_post_autor
        FOREIGN KEY (autor_id)
        REFERENCES usuario (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_post_pai
        FOREIGN KEY (post_pai_id)
        REFERENCES post (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_post_livro
        FOREIGN KEY (livro_id)
        REFERENCES livro (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_post_clube
        FOREIGN KEY (clube_id)
        REFERENCES clube_do_livro (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_post_comunidade
        FOREIGN KEY (comunidade_id)
        REFERENCES comunidade (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE INDEX idx_post_pai
    ON post (post_pai_id);

CREATE INDEX idx_post_livro
    ON post (livro_id);

CREATE INDEX idx_post_clube
    ON post (clube_id);

CREATE INDEX idx_post_comunidade
    ON post (comunidade_id);

CREATE TABLE curtida (
    id UUID NOT NULL,
    post_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    criada_em TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_curtida PRIMARY KEY (id),

    CONSTRAINT uq_curtida_post_usuario
        UNIQUE (post_id, usuario_id),

    CONSTRAINT fk_curtida_post
        FOREIGN KEY (post_id)
        REFERENCES post (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_curtida_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE TABLE seguir (
    id UUID NOT NULL,
    seguidor_id UUID NOT NULL,
    seguido_id UUID NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_seguir PRIMARY KEY (id),

    CONSTRAINT uq_seguir_seguidor_seguido
        UNIQUE (seguidor_id, seguido_id),

    CONSTRAINT chk_seguir_usuarios_diferentes
        CHECK (seguidor_id <> seguido_id),

    CONSTRAINT fk_seguir_seguidor
        FOREIGN KEY (seguidor_id)
        REFERENCES usuario (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_seguir_seguido
        FOREIGN KEY (seguido_id)
        REFERENCES usuario (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE INDEX idx_seguir_seguido
    ON seguir (seguido_id);

CREATE TABLE desafio_amigo (
    id UUID NOT NULL,
    criador_id UUID NOT NULL,
    oponente_id UUID NOT NULL,
    livro_id UUID,
    titulo VARCHAR(150) NOT NULL,
    descricao TEXT,
    tipo_meta tipo_meta_desafio NOT NULL,
    meta_valor INTEGER NOT NULL,
    recompensa_xp INTEGER NOT NULL DEFAULT 0,
    dificuldade VARCHAR(20),
    icone_url TEXT,
    status status_desafio NOT NULL DEFAULT 'pendente',
    inicio_em TIMESTAMPTZ NOT NULL,
    fim_em TIMESTAMPTZ NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_desafio_amigo PRIMARY KEY (id),

    CONSTRAINT chk_desafio_amigo_usuarios_diferentes
        CHECK (criador_id <> oponente_id),

    CONSTRAINT chk_desafio_amigo_meta_valor
        CHECK (meta_valor > 0),

    CONSTRAINT chk_desafio_amigo_recompensa_xp
        CHECK (recompensa_xp >= 0),

    CONSTRAINT chk_desafio_amigo_periodo
        CHECK (fim_em > inicio_em),

    CONSTRAINT fk_desafio_amigo_criador
        FOREIGN KEY (criador_id)
        REFERENCES usuario (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_desafio_amigo_oponente
        FOREIGN KEY (oponente_id)
        REFERENCES usuario (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_desafio_amigo_livro
        FOREIGN KEY (livro_id)
        REFERENCES livro (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE INDEX idx_desafio_amigo_criador
    ON desafio_amigo (criador_id);

CREATE INDEX idx_desafio_amigo_oponente
    ON desafio_amigo (oponente_id);

CREATE TABLE progresso_desafio (
    id UUID NOT NULL,
    desafio_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    valor_atual INTEGER NOT NULL DEFAULT 0,
    aceito_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_progresso_desafio PRIMARY KEY (id),

    CONSTRAINT uq_progresso_desafio_usuario
        UNIQUE (desafio_id, usuario_id),

    CONSTRAINT chk_progresso_desafio_valor_atual
        CHECK (valor_atual >= 0),

    CONSTRAINT fk_progresso_desafio_desafio
        FOREIGN KEY (desafio_id)
        REFERENCES desafio_amigo (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_progresso_desafio_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE TABLE conquista_usuario (
    id UUID NOT NULL,
    conquista_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    obtida_em TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_conquista_usuario PRIMARY KEY (id),

    CONSTRAINT uq_conquista_usuario
        UNIQUE (usuario_id, conquista_id),

    CONSTRAINT fk_conquista_usuario_conquista
        FOREIGN KEY (conquista_id)
        REFERENCES conquista (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION,

    CONSTRAINT fk_conquista_usuario_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
        ON DELETE NO ACTION
        ON UPDATE NO ACTION
);

CREATE TABLE quiz (
    id UUID NOT NULL,

    CONSTRAINT pk_quiz PRIMARY KEY (id)
);

CREATE TABLE mascote (
    id UUID NOT NULL,

    CONSTRAINT pk_mascote PRIMARY KEY (id)
);

CREATE TABLE evento (
    id UUID NOT NULL,

    CONSTRAINT pk_evento PRIMARY KEY (id)
);
