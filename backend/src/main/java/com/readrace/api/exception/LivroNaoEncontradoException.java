package com.readrace.api.exception;

import java.util.UUID;

public class LivroNaoEncontradoException extends ExcecaoDeNegocio {

    public LivroNaoEncontradoException(UUID livroId) {
        super(
                CodigoErro.LIVRO_NAO_ENCONTRADO,
                "Livro %s não encontrado.".formatted(livroId));
    }
}