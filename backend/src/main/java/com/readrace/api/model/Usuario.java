package com.readrace.api.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "nome_usuario", nullable = false, length = 50)
    private String nomeUsuario;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Column(nullable = false)
    private Integer nivel;

    public String getTitulo() {
        if (nivel <= 10) {
            return "Leitor iniciante";
        } else if (nivel <= 20) {
            return "Leitor explorador";
        } else if (nivel <= 30) {
            return "Leitor dedicado";
        } else if (nivel <= 40) {
            return "Leitor experiente";
        } else {
            return "Mestre da leitura";
        }
    }
}
