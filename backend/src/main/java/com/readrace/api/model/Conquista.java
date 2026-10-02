package com.readrace.api.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "conquista")
public class Conquista {

    @Id private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 30)
    private String criterio;

    @Column(name = "meta_valor", nullable = false)
    private Integer metaValor;

    @Column(name = "recompensa_xp", nullable = false)
    private Integer recompensaXp;

    protected Conquista() {}

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public CriterioConquista getCriterio() {
        return CriterioConquista.de(criterio);
    }

    public int getMetaValor() {
        return metaValor;
    }

    public int getRecompensaXp() {
        return recompensaXp;
    }

    public boolean foiAlcancada(long valorMedido) {
        return valorMedido >= metaValor;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Conquista outra)) {
            return false;
        }

        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
