package com.readrace.api.dto.response;

import java.util.UUID;

import com.readrace.api.model.SolicitacaoEntradaClube;

public record SolicitacaoClubeResponse(UUID id, String situacao) {
    public static SolicitacaoClubeResponse de(SolicitacaoEntradaClube solicitacao) {
        return new SolicitacaoClubeResponse(
                solicitacao.getId(), solicitacao.getSituacao().getValor());
    }
}
