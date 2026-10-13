package com.readrace.api.exception;

public class AlternativaInvalidaException extends ExcecaoDeNegocio {

    public AlternativaInvalidaException() {
        super(CodigoErro.ALTERNATIVA_INVALIDA, CodigoErro.ALTERNATIVA_INVALIDA.mensagemPadrao());
    }
}
