package com.readrace.api.exception;

/**
 * Lance quando um serviço externo do qual dependemos (ex.: Google Books API) falha ou não responde.
 *
 * <p>Vira HTTP 503 com {@code code = EXTERNAL_SERVICE_UNAVAILABLE}, para o cliente distinguir
 * "dependência externa fora do ar" de um erro interno nosso ({@code INTERNAL_ERROR}).
 */
public class ServicoExternoIndisponivelException extends ExcecaoDeNegocio {
    public ServicoExternoIndisponivelException(String mensagem) {
        super(CodigoErro.EXTERNAL_SERVICE_UNAVAILABLE, mensagem);
    }
}
