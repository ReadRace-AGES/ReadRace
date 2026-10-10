package com.readrace.api.exception;

public class NaoEMembroException extends ExcecaoDeNegocio {

    public NaoEMembroException() {
        super(
                CodigoErro.NAO_E_MEMBRO,
                CodigoErro.NAO_E_MEMBRO.mensagemPadrao());
    }
}