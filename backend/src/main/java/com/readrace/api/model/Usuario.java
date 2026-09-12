package com.readrace.api.model;

import java.time.OffsetDateTime;
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

    @Column(name = "excluido_em")
    private OffsetDateTime excluidoEm;

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
