package com.readrace.api.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "solicitacao_entrada_clube")
public class SolicitacaoEntradaClube {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "clube_id", nullable = false, updatable = false)
    private UUID clubeId;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "situacao_solicitacao_clube")
    private SituacaoSolicitacaoClube situacao;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private OffsetDateTime criadaEm;

    @Column(name = "respondida_em")
    private OffsetDateTime respondidaEm;

    @Column(name = "administrador_id")
    private UUID administradorId;

    public SolicitacaoEntradaClube(UUID clubeId, UUID usuarioId, OffsetDateTime criadaEm) {
        this.clubeId = Objects.requireNonNull(clubeId);
        this.usuarioId = Objects.requireNonNull(usuarioId);
        this.criadaEm = Objects.requireNonNull(criadaEm);
        this.situacao = SituacaoSolicitacaoClube.PENDENTE;
    }

    public void responder(
            SituacaoSolicitacaoClube resposta, OffsetDateTime momento, UUID administradorId) {
        if (situacao != SituacaoSolicitacaoClube.PENDENTE) {
            throw new IllegalStateException("A solicitação já foi respondida.");
        }
        Objects.requireNonNull(resposta);
        Objects.requireNonNull(momento);
        Objects.requireNonNull(administradorId);
        if (resposta == SituacaoSolicitacaoClube.PENDENTE) {
            throw new IllegalArgumentException("A resposta deve ser aceita ou recusada.");
        }
        this.situacao = resposta;
        this.respondidaEm = momento;
        this.administradorId = administradorId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SolicitacaoEntradaClube outra)) return false;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
