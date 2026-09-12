package com.readrace.api.exception;

public class LivroNaoEncontradoException extends ExcecaoDeNegocio {

    public LivroNaoEncontradoException() {
        super(CodigoErro.LIVRO_NAO_ENCONTRADO, CodigoErro.LIVRO_NAO_ENCONTRADO.mensagemPadrao());
    }
}
