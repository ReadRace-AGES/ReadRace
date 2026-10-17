package com.readrace.api.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "membro_comunidade")
public class MembroComunidade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comunidade_id", nullable = false)
    private Comunidade comunidade;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "cargo_comunidade", nullable = false, columnDefinition = "cargo")
    private Cargo cargoComunidade;

    public MembroComunidade(Comunidade comunidade, UUID usuarioId, Cargo cargoComunidade) {
        this.comunidade = comunidade;
        this.usuarioId = usuarioId;
        this.cargoComunidade = cargoComunidade;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof MembroComunidade outro)) {
            return false;
        }

        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
