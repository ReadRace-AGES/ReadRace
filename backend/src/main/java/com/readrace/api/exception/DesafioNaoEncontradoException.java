package com.readrace.api.exception;

public class DesafioNaoEncontradoException extends ExcecaoDeNegocio {

    public DesafioNaoEncontradoException() {
        super(
                CodigoErro.DESAFIO_NAO_ENCONTRADO,
                CodigoErro.DESAFIO_NAO_ENCONTRADO.mensagemPadrao());
    }
}
