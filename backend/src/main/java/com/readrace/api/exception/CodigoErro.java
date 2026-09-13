package com.readrace.api.exception;

import org.springframework.http.HttpStatus;

public enum CodigoErro {
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Recurso nao encontrado."),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "Rota nao encontrada."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dados invalidos."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Requisicao malformada."),
    PARAMETRO_INVALIDO(HttpStatus.BAD_REQUEST, "Parametro de busca invalido."),
    PAGINA_INVALIDA(HttpStatus.UNPROCESSABLE_ENTITY, "Pagina invalida."),
    DESAFIO_NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Desafio nao encontrado."),
    OPONENTE_NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Oponente nao encontrado."),
    LIVRO_NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Livro nao encontrado."),
    OPONENTE_NAO_E_AMIGO(
            HttpStatus.BAD_REQUEST, "O oponente informado nao e amigo do usuario atual."),
    META_INVALIDA(HttpStatus.BAD_REQUEST, "A meta de paginas deve estar entre 10 e 500."),
    LIVRO_OBRIGATORIO(
            HttpStatus.BAD_REQUEST, "O livro e obrigatorio para desafios com meta por livro."),
    PRAZO_INVALIDO(HttpStatus.BAD_REQUEST, "O prazo deve ser maior que zero."),
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
