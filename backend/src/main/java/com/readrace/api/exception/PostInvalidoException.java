package com.readrace.api.exception;

public class PostInvalidoException extends ExcecaoDeNegocio {
    public PostInvalidoException() {
        super(CodigoErro.POST_INVALIDO, CodigoErro.POST_INVALIDO.mensagemPadrao());
    }
}
