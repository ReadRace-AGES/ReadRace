package com.readrace.api.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "post")
public class Post {

    @Id
    private UUID id;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(name = "livro_id")
    private UUID livroId;

    @Column(name = "post_pai_id")
    private UUID postPaiId;

    @Column(nullable = false)
    private String conteudo;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "excluido_em")
    private OffsetDateTime excluidoEm;

    protected Post() {}

    public UUID getId() {
        return id;
    }

    public UUID getAutorId() {
        return autorId;
    }

    public UUID getLivroId() {
        return livroId;
    }

    public UUID getPostPaiId() {
        return postPaiId;
    }

    public String getConteudo() {
        return conteudo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getExcluidoEm() {
        return excluidoEm;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Post outro)) {
            return false;
        }

        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}