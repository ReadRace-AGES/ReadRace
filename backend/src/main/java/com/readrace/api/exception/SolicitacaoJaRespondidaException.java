package com.readrace.api.exception;

public class SolicitacaoJaRespondidaException extends ExcecaoDeNegocio {
    public SolicitacaoJaRespondidaException() {
        super(
                CodigoErro.SOLICITACAO_JA_RESPONDIDA,
                CodigoErro.SOLICITACAO_JA_RESPONDIDA.mensagemPadrao());
    }
}
