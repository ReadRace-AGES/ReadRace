package com.readrace.api.exception;

public class LivroObrigatorioException extends ExcecaoDeNegocio {

    public LivroObrigatorioException() {
        super(CodigoErro.LIVRO_OBRIGATORIO, CodigoErro.LIVRO_OBRIGATORIO.mensagemPadrao());
    }
}
