package com.readrace.api.exception;

public class PrazoInvalidoException extends ExcecaoDeNegocio {

    public PrazoInvalidoException() {
        super(CodigoErro.PRAZO_INVALIDO, CodigoErro.PRAZO_INVALIDO.mensagemPadrao());
    }
}
