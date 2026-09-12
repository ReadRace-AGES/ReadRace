package com.readrace.api.exception;

import org.springframework.http.HttpStatus;

public enum CodigoErro {
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Recurso não encontrado."),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "Rota não encontrada."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dados inválidos."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Requisição malformada."),
    PARAMETRO_INVALIDO(HttpStatus.BAD_REQUEST, "Parâmetro de busca inválido."),
    DESAFIO_NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Desafio não encontrado."),
    OPONENTE_NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Oponente não encontrado."),
    LIVRO_NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Livro não encontrado."),
    OPONENTE_NAO_E_AMIGO(
            HttpStatus.BAD_REQUEST, "O oponente informado não é amigo do usuário atual."),
    META_INVALIDA(HttpStatus.BAD_REQUEST, "A meta de páginas deve estar entre 10 e 500."),
    LIVRO_OBRIGATORIO(
            HttpStatus.BAD_REQUEST, "O livro é obrigatório para desafios com meta por livro."),
    PRAZO_INVALIDO(HttpStatus.BAD_REQUEST, "O prazo deve ser maior que zero."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido para esta rota."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato de conteúdo não suportado."),
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
            default -> INTERNAL_ERROR;
        };
    }
}
