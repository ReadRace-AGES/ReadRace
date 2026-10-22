package com.readrace.api.exception;

public class MascoteBloqueadoException extends ExcecaoDeNegocio {

    public MascoteBloqueadoException() {
        super(CodigoErro.MASCOTE_BLOQUEADO, CodigoErro.MASCOTE_BLOQUEADO.mensagemPadrao());
    }
}
