package com.readrace.api.exception;

public class QuizInvalidoException extends ExcecaoDeNegocio {

    public QuizInvalidoException(String mensagem) {
        super(CodigoErro.QUIZ_INVALIDO, mensagem);
    }
}
