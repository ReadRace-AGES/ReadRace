package com.readrace.api.exception;

public class LivroComLeituraException extends ExcecaoDeNegocio {

    public LivroComLeituraException() {
        super(CodigoErro.LIVRO_COM_LEITURA, CodigoErro.LIVRO_COM_LEITURA.mensagemPadrao());
    }
}
