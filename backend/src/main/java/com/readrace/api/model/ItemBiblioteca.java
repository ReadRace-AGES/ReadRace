package com.readrace.api.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "item_biblioteca")
public class ItemBiblioteca {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "livro_id", nullable = false)
    private UUID livroId;

    @Column(name = "pagina_atual", nullable = false)
    private Integer paginaAtual;

    @Column(name = "pagina_maxima", nullable = false)
    private Integer paginaMaxima;

    protected ItemBiblioteca() {}

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public UUID getLivroId() {
        return livroId;
    }

    public Integer getPaginaAtual() {
        return paginaAtual;
    }

    public Integer getPaginaMaxima() {
        return paginaMaxima;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ItemBiblioteca outro)) {
            return false;
        }

        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}