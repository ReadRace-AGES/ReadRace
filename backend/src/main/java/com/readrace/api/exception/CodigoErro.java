package com.readrace.api.exception;

import org.springframework.http.HttpStatus;

public enum CodigoErro {
    NAO_PODE_SEGUIR_A_SI(HttpStatus.BAD_REQUEST, "Você não pode seguir a si mesmo."),
    USUARIO_NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Usuário não encontrado."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Recurso não encontrado."),
    ROUTE_NOT_FOUND(HttpStatus.NOT_FOUND, "Rota não encontrada."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dados inválidos."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Requisição malformada."),
    PARAMETRO_INVALIDO(HttpStatus.BAD_REQUEST, "Parâmetro de busca inválido."),
    PAGINA_INVALIDA(HttpStatus.UNPROCESSABLE_ENTITY, "Página inválida."),
    DESAFIO_NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Desafio não encontrado."),
    OPONENTE_NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Oponente não encontrado."),
    LIVRO_NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Livro não encontrado."),
    COMUNIDADE_NAO_ENCONTRADA(HttpStatus.NOT_FOUND, "Comunidade não encontrada."),
    POST_NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Post não encontrado."),
    LISTA_INVALIDA(HttpStatus.BAD_REQUEST, "Lista inválida. Use lido, desejo ou favorito."),
    LIVRO_SEM_ISBN_OU_PAGINAS(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "O livro não tem ISBN ou número de páginas suficiente para entrar na biblioteca."),
    POST_INVALIDO(
            HttpStatus.BAD_REQUEST, "O post deve conter texto e ter no máximo 2.000 caracteres."),
    SO_MEMBRO_POSTA(HttpStatus.FORBIDDEN, "Somente membros do clube podem publicar no fórum."),
    SO_MEMBRO_VE_RANKING(HttpStatus.FORBIDDEN, "Só membros do clube veem o ranking completo."),

    // Chat do clube - Task #151
    CLUBE_NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Clube não encontrado."),
    NAO_E_MEMBRO(HttpStatus.NOT_FOUND, "Você não é membro deste clube."),
    UNICO_LIDER(HttpStatus.CONFLICT, "Você é o único líder deste clube e não pode sair."),
    MENSAGEM_INVALIDA(HttpStatus.BAD_REQUEST, "Mensagem inválida."),
    SO_MEMBRO_NO_CHAT(HttpStatus.FORBIDDEN, "Só membros do clube participam do chat."),

    OPONENTE_NAO_E_AMIGO(
            HttpStatus.BAD_REQUEST, "O oponente informado não é amigo do usuário atual."),
    CLUBE_INVALIDO(
            HttpStatus.BAD_REQUEST, "O nome do clube é obrigatório e deve ter até 120 caracteres."),
    META_INVALIDA(HttpStatus.BAD_REQUEST, "A meta de páginas deve estar entre 10 e 500."),
    LIVRO_OBRIGATORIO(
            HttpStatus.BAD_REQUEST, "O livro é obrigatório para desafios com meta por livro."),
    PRAZO_INVALIDO(HttpStatus.BAD_REQUEST, "O prazo deve ser de 1 a 365 dias."),
    PERFIL_INVALIDO(HttpStatus.BAD_REQUEST, "Dados do perfil inválidos."),
    USERNAME_EM_USO(HttpStatus.CONFLICT, "Esse nome de usuário já está em uso."),
    UNICO_ADMINISTRADOR(
            HttpStatus.CONFLICT, "Você é o único administrador desta comunidade e não pode sair."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada. Entre de novo."),
    QUIZ_NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Este clube ainda não tem quiz."),
    SO_MEMBRO_RESPONDE(HttpStatus.FORBIDDEN, "Só membros do clube participam do quiz."),
    PERGUNTA_JA_RESPONDIDA(HttpStatus.CONFLICT, "Você já respondeu esta pergunta."),
    ALTERNATIVA_INVALIDA(HttpStatus.BAD_REQUEST, "A alternativa não pertence a esta pergunta."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido para esta rota."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato de conteúdo não suportado."),
    EXTERNAL_SERVICE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Serviço externo indisponível no momento. Tente novamente mais tarde."),
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
            case UNAUTHORIZED -> UNAUTHORIZED;
            case METHOD_NOT_ALLOWED -> METHOD_NOT_ALLOWED;
            case UNSUPPORTED_MEDIA_TYPE -> UNSUPPORTED_MEDIA_TYPE;
            case BAD_REQUEST -> MALFORMED_REQUEST;
            case BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT -> EXTERNAL_SERVICE_UNAVAILABLE;
            default -> INTERNAL_ERROR;
        };
    }
}
