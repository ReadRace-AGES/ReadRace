package com.readrace.api.exception;

public class OponenteNaoEAmigoException extends ExcecaoDeNegocio {

    public OponenteNaoEAmigoException() {
        super(CodigoErro.OPONENTE_NAO_E_AMIGO, CodigoErro.OPONENTE_NAO_E_AMIGO.mensagemPadrao());
    }
}
