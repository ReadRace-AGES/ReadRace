package com.readrace.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SolicitacaoEntradaClubeTest {
    private final OffsetDateTime agora = OffsetDateTime.parse("2026-10-30T12:00:00Z");
    private final UUID administrador = UUID.randomUUID();

    @ParameterizedTest
    @EnumSource(
            value = SituacaoSolicitacaoClube.class,
            names = {"ACEITA", "RECUSADA"})
    void responde_uma_unica_vez(SituacaoSolicitacaoClube resposta) {
        var solicitacao = new SolicitacaoEntradaClube(UUID.randomUUID(), UUID.randomUUID(), agora);
        assertThat(solicitacao.getSituacao()).isEqualTo(SituacaoSolicitacaoClube.PENDENTE);
        assertThat(solicitacao.getRespondidaEm()).isNull();
        assertThat(solicitacao.getAdministradorId()).isNull();

        solicitacao.responder(resposta, agora.plusHours(1), administrador);
        assertThatThrownBy(
                        () -> solicitacao.responder(resposta, agora.plusDays(1), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(solicitacao.getSituacao()).isEqualTo(resposta);
        assertThat(solicitacao.getRespondidaEm()).isEqualTo(agora.plusHours(1));
        assertThat(solicitacao.getAdministradorId()).isEqualTo(administrador);
    }

    @Test
    void rejeita_respostas_incompletas_sem_alterar_estado() {
        var solicitacao = new SolicitacaoEntradaClube(UUID.randomUUID(), UUID.randomUUID(), agora);
        assertThatThrownBy(
                        () ->
                                solicitacao.responder(
                                        SituacaoSolicitacaoClube.PENDENTE, agora, administrador))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> solicitacao.responder(null, agora, administrador))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(
                        () ->
                                solicitacao.responder(
                                        SituacaoSolicitacaoClube.ACEITA, null, administrador))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(
                        () -> solicitacao.responder(SituacaoSolicitacaoClube.RECUSADA, agora, null))
                .isInstanceOf(NullPointerException.class);
        assertThat(solicitacao.getSituacao()).isEqualTo(SituacaoSolicitacaoClube.PENDENTE);
        assertThat(solicitacao.getRespondidaEm()).isNull();
        assertThat(solicitacao.getAdministradorId()).isNull();
    }

    @Test
    void exige_dados_da_criacao() {
        assertThatThrownBy(() -> new SolicitacaoEntradaClube(null, administrador, agora))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SolicitacaoEntradaClube(administrador, null, agora))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SolicitacaoEntradaClube(administrador, administrador, null))
                .isInstanceOf(NullPointerException.class);
    }
}
