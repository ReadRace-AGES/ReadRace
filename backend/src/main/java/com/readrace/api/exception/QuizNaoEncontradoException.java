package com.readrace.api.exception;

public class QuizNaoEncontradoException extends ExcecaoDeNegocio {

    public QuizNaoEncontradoException() {
        super(CodigoErro.QUIZ_NAO_ENCONTRADO, CodigoErro.QUIZ_NAO_ENCONTRADO.mensagemPadrao());
    }
}
