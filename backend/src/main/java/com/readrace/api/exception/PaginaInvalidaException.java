package com.readrace.api.exception;

public class PaginaInvalidaException extends ExcecaoDeNegocio {

    public PaginaInvalidaException(String mensagem) {
        super(CodigoErro.PAGINA_INVALIDA, mensagem);
    }
}
