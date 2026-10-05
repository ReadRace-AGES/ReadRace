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
@Table(name = "membro_clube")
public class MembroClube {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clube_id", nullable = false)
    private ClubeDoLivro clube;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    /** `Pontos` do membro dentro deste clube — a moeda do ranking (#35). Nunca XP. */
    @Column(nullable = false)
    private Integer pontos;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "cargo_clube", nullable = false, columnDefinition = "cargo")
    
    private CargoClube cargoClube;

    public MembroClube(ClubeDoLivro clube, UUID usuarioId, CargoClube cargoClube) {
        this.clube = clube;
        this.usuarioId = usuarioId;
        this.cargoClube = cargoClube;
        this.pontos = 0;
    }

    public void somarPontos(int pontosGanhos) {
        pontos += pontosGanhos;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof MembroClube outro)) {
            return false;
        }

        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
