package com.readrace.api.exception;

/**
 * Lance quando um id pedido pelo cliente não existe no banco. Vira HTTP 404 com {@code code:
 * RESOURCE_NOT_FOUND}.
 *
 * <p>A mensagem passada aqui vai para o {@code message} da resposta, então escreva algo que faça
 * sentido para o usuário final — não um dump técnico.
 */
public class RecursoNaoEncontradoException extends ExcecaoDeNegocio {

    public RecursoNaoEncontradoException(String mensagem) {
        super(CodigoErro.RESOURCE_NOT_FOUND, mensagem);
    }
}
