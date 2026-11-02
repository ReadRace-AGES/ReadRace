package com.readrace.api.exception;

public class SoMembroPostaException extends ExcecaoDeNegocio {
    public SoMembroPostaException() {
        this(CodigoErro.SO_MEMBRO_POSTA.mensagemPadrao());
    }

    public SoMembroPostaException(String mensagem) {
        super(CodigoErro.SO_MEMBRO_POSTA, mensagem);
    }
}
