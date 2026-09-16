package com.readrace.api.exception;

public class OponenteNaoEncontradoException extends ExcecaoDeNegocio {

    public OponenteNaoEncontradoException() {
        super(
                CodigoErro.OPONENTE_NAO_ENCONTRADO,
                CodigoErro.OPONENTE_NAO_ENCONTRADO.mensagemPadrao());
    }
}
