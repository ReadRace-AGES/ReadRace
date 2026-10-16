package com.readrace.api.exception;

public class SoLiderCriaQuizException extends ExcecaoDeNegocio {

    public SoLiderCriaQuizException() {
        super(CodigoErro.SO_LIDER_CRIA_QUIZ, CodigoErro.SO_LIDER_CRIA_QUIZ.mensagemPadrao());
    }
}
