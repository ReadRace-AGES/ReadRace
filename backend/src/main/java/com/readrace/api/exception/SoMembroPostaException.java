package com.readrace.api.exception;

public class SoMembroPostaException extends ExcecaoDeNegocio {
    public SoMembroPostaException() {
        super(CodigoErro.SO_MEMBRO_POSTA, CodigoErro.SO_MEMBRO_POSTA.mensagemPadrao());
    }
}
