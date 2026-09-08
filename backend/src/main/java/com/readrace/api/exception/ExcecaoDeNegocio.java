package com.readrace.api.exception;

public abstract class ExcecaoDeNegocio extends RuntimeException {
    private final CodigoErro codigo;

    protected ExcecaoDeNegocio(CodigoErro codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
