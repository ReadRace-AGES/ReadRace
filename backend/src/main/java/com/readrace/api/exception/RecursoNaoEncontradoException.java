package com.readrace.api.exception;

public class RecursoNaoEncontradoException extends ExcecaoDeNegocio {
    public RecursoNaoEncontradoException(String mensagem) {
        super(CodigoErro.RESOURCE_NOT_FOUND, mensagem);
    }
}
