package com.readrace.api.exception;

/**
 * Lance quando um id pedido pelo cliente não existe no banco. O GlobalExceptionHandler transforma
 * isso em HTTP 404.
 */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
