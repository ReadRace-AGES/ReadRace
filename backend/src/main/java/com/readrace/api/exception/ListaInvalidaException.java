package com.readrace.api.exception;

public class ListaInvalidaException extends ExcecaoDeNegocio {

    public ListaInvalidaException() {
        super(CodigoErro.LISTA_INVALIDA, CodigoErro.LISTA_INVALIDA.mensagemPadrao());
    }
}
