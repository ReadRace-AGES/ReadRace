package com.readrace.api.exception;

public class SoMembroPostaException extends ExcecaoDeNegocio {
    public SoMembroPostaException() {
        this(CodigoErro.SO_MEMBRO_POSTA.mensagemPadrao());
    }

    /** Mesmo código, com a mensagem ajustada à ação (ex.: curtir em vez de publicar). */
    public SoMembroPostaException(String mensagem) {
        super(CodigoErro.SO_MEMBRO_POSTA, mensagem);
    }
}
