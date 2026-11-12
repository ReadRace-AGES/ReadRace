package com.readrace.api.exception;

public class LivroEmProgressoException extends ExcecaoDeNegocio {

    public LivroEmProgressoException() {
        super(CodigoErro.LIVRO_EM_PROGRESSO, CodigoErro.LIVRO_EM_PROGRESSO.mensagemPadrao());
    }
}
