package com.readrace.api.exception;

public class ComunidadeNaoEncontradaException extends ExcecaoDeNegocio {
    public ComunidadeNaoEncontradaException() {
        super(
                CodigoErro.COMUNIDADE_NAO_ENCONTRADA,
                CodigoErro.COMUNIDADE_NAO_ENCONTRADA.mensagemPadrao());
    }
}
