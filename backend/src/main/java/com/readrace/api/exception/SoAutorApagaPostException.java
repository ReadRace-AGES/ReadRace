package com.readrace.api.exception;

public class SoAutorApagaPostException extends ExcecaoDeNegocio {

    public SoAutorApagaPostException() {
        super(CodigoErro.SO_AUTOR_APAGA_POST, CodigoErro.SO_AUTOR_APAGA_POST.mensagemPadrao());
    }
}
