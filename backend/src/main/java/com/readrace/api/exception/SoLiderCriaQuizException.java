package com.readrace.api.exception;

public class SoLiderCriaQuizException extends ExcecaoDeNegocio {

    public SoLiderCriaQuizException() {
        this(CodigoErro.SO_LIDER_CRIA_QUIZ.mensagemPadrao());
    }

    public SoLiderCriaQuizException(String mensagem) {
        super(CodigoErro.SO_LIDER_CRIA_QUIZ, mensagem);
    }
}
