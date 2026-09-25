package com.readrace.api.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Curtida de um usuário em um post (#101). Gravada/removida por SQL nativo em {@link
 * com.readrace.api.repository.CurtidaRepository} — a restrição única (post_id, usuario_id) é quem
 * garante a idempotência, então esta entidade existe para o Hibernate validar a tabela.
 */
@Entity
@Table(name = "curtida")
public class Curtida {

    @Id private UUID id;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "criada_em", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime criadaEm;

    protected Curtida() {}

    public UUID getId() {
        return id;
    }

    public UUID getPostId() {
        return postId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public OffsetDateTime getCriadaEm() {
        return criadaEm;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Curtida outra)) {
            return false;
        }

        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
