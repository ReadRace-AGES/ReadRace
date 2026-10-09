package com.readrace.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mensagem_clube")
public class MensagemClube {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clube_id", nullable = false)
    private ClubeDoLivro clube;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(name = "texto", nullable = false, length = 1000)
    private String texto;

    @Column(name = "enviada_em", nullable = false)
    private OffsetDateTime enviadaEm;

    protected MensagemClube() {
    }

    public MensagemClube(
            UUID id,
            ClubeDoLivro clube,
            Usuario autor,
            String texto,
            OffsetDateTime enviadaEm) {
        this.id = id;
        this.clube = clube;
        this.autor = autor;
        this.texto = texto;
        this.enviadaEm = enviadaEm;
    }

    public UUID getId() {
        return id;
    }

    public ClubeDoLivro getClube() {
        return clube;
    }

    public Usuario getAutor() {
        return autor;
    }

    public String getTexto() {
        return texto;
    }

    public OffsetDateTime getEnviadaEm() {
        return enviadaEm;
    }
}