package com.readrace.api.exception;

public class SoMembroVeRankingException extends ExcecaoDeNegocio {
    public SoMembroVeRankingException() {
        super(CodigoErro.SO_MEMBRO_VE_RANKING, CodigoErro.SO_MEMBRO_VE_RANKING.mensagemPadrao());
    }
}
