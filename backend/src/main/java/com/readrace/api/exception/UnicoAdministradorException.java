package com.readrace.api.exception;

public class UnicoAdministradorException extends ExcecaoDeNegocio {
    public UnicoAdministradorException() {
        super(CodigoErro.UNICO_ADMINISTRADOR, "");
    }
}
