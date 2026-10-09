package com.readrace.api.exception;

public class MensagemInvalidaException extends ExcecaoDeNegocio {

    public MensagemInvalidaException() {
        super(
                CodigoErro.MENSAGEM_INVALIDA,
                CodigoErro.MENSAGEM_INVALIDA.mensagemPadrao());
    }
}