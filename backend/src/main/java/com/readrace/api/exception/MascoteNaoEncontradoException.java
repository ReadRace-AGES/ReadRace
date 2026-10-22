package com.readrace.api.exception;

public class MascoteNaoEncontradoException extends ExcecaoDeNegocio {

    public MascoteNaoEncontradoException() {
        super(
                CodigoErro.MASCOTE_NAO_ENCONTRADO,
                CodigoErro.MASCOTE_NAO_ENCONTRADO.mensagemPadrao());
    }
}
