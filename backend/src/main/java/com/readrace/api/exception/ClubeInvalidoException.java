package com.readrace.api.exception;

public class ClubeInvalidoException extends ExcecaoDeNegocio {
    public ClubeInvalidoException() {
        super(CodigoErro.CLUBE_INVALIDO, CodigoErro.CLUBE_INVALIDO.mensagemPadrao());
    }
}
