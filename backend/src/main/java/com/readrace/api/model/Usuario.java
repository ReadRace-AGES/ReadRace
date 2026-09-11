package com.readrace.api.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "dias_consecutivos", nullable = false)
    private Integer diasConsecutivos;

    protected Usuario() {}

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Integer getDiasConsecutivos() {
        return diasConsecutivos;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Usuario outro)) {
            return false;
        }

        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
