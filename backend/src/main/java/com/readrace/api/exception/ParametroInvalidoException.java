package com.readrace.api.exception;

public class ParametroInvalidoException extends ExcecaoDeNegocio {

    public ParametroInvalidoException(String mensagem) {
        super(CodigoErro.PARAMETRO_INVALIDO, mensagem);
    }
}
