package com.readrace.api.exception;

public class PerfilInvalidoException extends ExcecaoDeNegocio {

    public PerfilInvalidoException(String mensagem) {
        super(CodigoErro.PERFIL_INVALIDO, mensagem);
    }
}
