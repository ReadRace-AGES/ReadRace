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
@Table(name = "comunidade")
public class Comunidade {

    @Id private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(name = "imagem_url", columnDefinition = "text")
    private String imagemUrl;

    @Column(name = "excluido_em")
    private OffsetDateTime excluidoEm;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Comunidade outra)) {
            return false;
        }

        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
