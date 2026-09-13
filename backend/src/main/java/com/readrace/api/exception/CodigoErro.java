package com.readrace.api.exception;

import org.springframework.http.HttpStatus;

public enum CodigoErro {
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Recurso nao encontrado."),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "Rota nao encontrada."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dados invalidos."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Requisicao malformada."),
    PARAMETRO_INVALIDO(HttpStatus.BAD_REQUEST, "Parametro de busca invalido."),
    PAGINA_INVALIDA(HttpStatus.UNPROCESSABLE_ENTITY, "Pagina invalida."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Metodo nao permitido para esta rota."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato de conteudo nao suportado."),
    EXTERNAL_SERVICE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Servico externo indisponivel no momento. Tente novamente mais tarde."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente.");

    private final HttpStatus status;

    private final String mensagemPadrao;

    CodigoErro(HttpStatus status, String mensagemPadrao) {
        this.status = status;
        this.mensagemPadrao = mensagemPadrao;
    }

    public HttpStatus status() {
        return status;
    }

    public String mensagemPadrao() {
        return mensagemPadrao;
    }

    public static CodigoErro paraStatus(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> ROUTE_NOT_FOUND;
            case METHOD_NOT_ALLOWED -> METHOD_NOT_ALLOWED;
            case UNSUPPORTED_MEDIA_TYPE -> UNSUPPORTED_MEDIA_TYPE;
            case BAD_REQUEST -> MALFORMED_REQUEST;
            case BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT -> EXTERNAL_SERVICE_UNAVAILABLE;
            default -> INTERNAL_ERROR;
        };
    }
}
