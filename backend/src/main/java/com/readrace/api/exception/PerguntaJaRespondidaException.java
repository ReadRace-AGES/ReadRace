package com.readrace.api.exception;

public class PerguntaJaRespondidaException extends ExcecaoDeNegocio {

    public PerguntaJaRespondidaException() {
        super(
                CodigoErro.PERGUNTA_JA_RESPONDIDA,
                CodigoErro.PERGUNTA_JA_RESPONDIDA.mensagemPadrao());
    }
}
