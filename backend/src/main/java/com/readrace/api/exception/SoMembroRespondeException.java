package com.readrace.api.exception;

public class SoMembroRespondeException extends ExcecaoDeNegocio {

    public SoMembroRespondeException() {
        super(CodigoErro.SO_MEMBRO_RESPONDE, CodigoErro.SO_MEMBRO_RESPONDE.mensagemPadrao());
    }
}
