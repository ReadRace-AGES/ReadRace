package com.readrace.api.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "nome_usuario", nullable = false, length = 50)
    private String nomeUsuario;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    protected Usuario() {}

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}