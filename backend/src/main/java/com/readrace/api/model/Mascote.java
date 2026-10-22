package com.readrace.api.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "mascote")
public class Mascote {

    @Id private UUID id;

    @Column(nullable = false, length = 60)
    private String nome;

    /** Chave da arte; o app traduz para o arquivo empacotado. */
    @Column(nullable = false, length = 60)
    private String imagem;

    @Column(name = "nivel_minimo", nullable = false)
    private Integer nivelMinimo;

    @Column(nullable = false)
    private Short ordem;

    protected Mascote() {}

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getImagem() {
        return imagem;
    }

    public int getNivelMinimo() {
        return nivelMinimo;
    }

    public boolean desbloqueadoNoNivel(int nivel) {
        return nivel >= nivelMinimo;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Mascote outro)) {
            return false;
        }

        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
