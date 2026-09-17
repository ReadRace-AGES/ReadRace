package com.readrace.api.exception;

public class UsuarioNaoEncontradoException extends ExcecaoDeNegocio {
    public UsuarioNaoEncontradoException() {
        super(
                CodigoErro.USUARIO_NAO_ENCONTRADO,
                CodigoErro.USUARIO_NAO_ENCONTRADO.mensagemPadrao());
    }
}
