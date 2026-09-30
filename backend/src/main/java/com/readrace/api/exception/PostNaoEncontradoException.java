package com.readrace.api.exception;

public class PostNaoEncontradoException extends ExcecaoDeNegocio {

    public PostNaoEncontradoException() {
        super(CodigoErro.POST_NAO_ENCONTRADO, CodigoErro.POST_NAO_ENCONTRADO.mensagemPadrao());
    }
}
