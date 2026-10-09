package com.readrace.api.exception;

public class SoMembroNoChatException extends ExcecaoDeNegocio {

    public SoMembroNoChatException() {
        super(
                CodigoErro.SO_MEMBRO_NO_CHAT,
                CodigoErro.SO_MEMBRO_NO_CHAT.mensagemPadrao());
    }
}