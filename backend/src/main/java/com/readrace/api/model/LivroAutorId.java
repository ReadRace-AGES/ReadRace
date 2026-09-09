package com.readrace.api.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class LivroAutorId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "livro_id", nullable = false)
    private UUID livroId;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    protected LivroAutorId() {}

    public LivroAutorId(UUID livroId, UUID autorId) {
        this.livroId = livroId;
        this.autorId = autorId;
    }

    public UUID getLivroId() {
        return livroId;
    }

    public UUID getAutorId() {
        return autorId;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }

        if (!(objeto instanceof LivroAutorId outro)) {
            return false;
        }

        return Objects.equals(livroId, outro.livroId) && Objects.equals(autorId, outro.autorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(livroId, autorId);
    }
}
