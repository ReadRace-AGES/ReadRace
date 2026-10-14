package com.readrace.api.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Quiz de um clube do livro (V13). Só um fica ativo por clube; os inativos não são apagados. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "quiz")
public class Quiz {

    @Id
    private UUID id;

    @Column(name = "clube_id", nullable = false)
    private UUID clubeId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "livro_id", nullable = false)
    private Livro livro;

    @Column(name = "criado_por", nullable = false)
    private UUID criadoPor;

    @Column(nullable = false, length = 150)
    private String titulo;

    /** XP pago uma única vez a quem conclui o quiz. Nunca vira Pontos. */
    @Column(name = "recompensa_xp", nullable = false)
    private Integer recompensaXp;

    @Column(nullable = false)
    private Boolean ativo;

    public Quiz(
            UUID clubeId,
            Livro livro,
            UUID criadoPor,
            String titulo,
            Integer recompensaXp) {
        this.id = UUID.randomUUID();
        this.clubeId = clubeId;
        this.livro = livro;
        this.criadoPor = criadoPor;
        this.titulo = titulo;
        this.recompensaXp = recompensaXp;
        this.ativo = true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Quiz outro)) {
            return false;
        }

        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
