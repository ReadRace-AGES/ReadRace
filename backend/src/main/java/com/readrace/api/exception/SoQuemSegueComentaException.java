package com.readrace.api.exception;

public class SoQuemSegueComentaException extends ExcecaoDeNegocio {
    public SoQuemSegueComentaException() {
        super(CodigoErro.SO_QUEM_SEGUE_COMENTA, CodigoErro.SO_QUEM_SEGUE_COMENTA.mensagemPadrao());
    }
}
