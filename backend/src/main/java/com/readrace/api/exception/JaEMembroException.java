package com.readrace.api.exception;

public class JaEMembroException extends ExcecaoDeNegocio {
    public JaEMembroException() {
        super(CodigoErro.JA_E_MEMBRO, CodigoErro.JA_E_MEMBRO.mensagemPadrao());
    }
}
