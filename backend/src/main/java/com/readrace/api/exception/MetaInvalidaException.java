package com.readrace.api.exception;

public class MetaInvalidaException extends ExcecaoDeNegocio {

    public MetaInvalidaException() {
        super(CodigoErro.META_INVALIDA, CodigoErro.META_INVALIDA.mensagemPadrao());
    }
}
