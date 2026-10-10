package com.readrace.api.exception;

public class UnicoLiderException extends ExcecaoDeNegocio {

    public UnicoLiderException() {
        super(
                CodigoErro.UNICO_LIDER,
                CodigoErro.UNICO_LIDER.mensagemPadrao());
    }
}