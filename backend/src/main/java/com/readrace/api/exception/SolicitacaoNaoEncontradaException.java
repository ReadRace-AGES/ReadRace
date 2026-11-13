package com.readrace.api.exception;

public class SolicitacaoNaoEncontradaException extends ExcecaoDeNegocio {
    public SolicitacaoNaoEncontradaException() {
        super(
                CodigoErro.SOLICITACAO_NAO_ENCONTRADA,
                CodigoErro.SOLICITACAO_NAO_ENCONTRADA.mensagemPadrao());
    }
}
