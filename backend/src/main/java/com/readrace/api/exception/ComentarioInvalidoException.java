package com.readrace.api.exception;

public class ComentarioInvalidoException extends ExcecaoDeNegocio {
    public ComentarioInvalidoException() {
        super(CodigoErro.COMENTARIO_INVALIDO, CodigoErro.COMENTARIO_INVALIDO.mensagemPadrao());
    }
}
