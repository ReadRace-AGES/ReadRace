package com.readrace.api.exception;

public class UsernameEmUsoException extends ExcecaoDeNegocio {

    public UsernameEmUsoException() {
        super(CodigoErro.USERNAME_EM_USO, CodigoErro.USERNAME_EM_USO.mensagemPadrao());
    }
}
