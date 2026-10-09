package com.readrace.api.exception;

public class ClubeNaoEncontradoException extends ExcecaoDeNegocio {

    public ClubeNaoEncontradoException() {
        super(
                CodigoErro.CLUBE_NAO_ENCONTRADO,
                CodigoErro.CLUBE_NAO_ENCONTRADO.mensagemPadrao());
    }
}