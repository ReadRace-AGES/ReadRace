package com.readrace.api.exception;

public class NaoPodeSeguirASiException extends ExcecaoDeNegocio {
    public NaoPodeSeguirASiException() {
        super(CodigoErro.NAO_PODE_SEGUIR_A_SI, CodigoErro.NAO_PODE_SEGUIR_A_SI.mensagemPadrao());
    }
}
