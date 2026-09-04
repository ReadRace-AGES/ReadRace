package com.readrace.api.dto.response;

import com.readrace.api.exception.CodigoErro;

public record ErroResponse(String code, String message) {

    public static ErroResponse de(CodigoErro codigo, String mensagem) {
        return new ErroResponse(codigo.name(), mensagem);
    }

    public static ErroResponse de(CodigoErro codigo) {
        return de(codigo, codigo.mensagemPadrao());
    }
}
