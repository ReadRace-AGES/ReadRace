package com.readrace.api.dto.response;

import com.readrace.api.exception.CodigoErro;

/**
 * O corpo de TODA resposta de erro da API, conforme a issue #10.
 *
 * <p>Duas chaves, sempre as mesmas:
 *
 * <pre>
 * {"code": "RESOURCE_NOT_FOUND", "message": "Exemplo 999 não encontrado"}
 * </pre>
 *
 * @param code identificador estável e legível por máquina. É nele que o mobile faz {@code if} — por
 *     isso NUNCA muda depois de publicado, mesmo que a mensagem mude
 * @param message texto em português, pronto para ser exibido na tela do usuário
 */
public record ErroResponse(String code, String message) {

    public static ErroResponse de(CodigoErro codigo, String mensagem) {
        return new ErroResponse(codigo.name(), mensagem);
    }

    public static ErroResponse de(CodigoErro codigo) {
        return de(codigo, codigo.mensagemPadrao());
    }
}
