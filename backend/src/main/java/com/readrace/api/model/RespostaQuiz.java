package com.readrace.api.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Uma por usuário por pergunta ({@code uq_resposta_quiz_usuario}). */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "resposta_quiz")
public class RespostaQuiz {

    @Id private UUID id;

    @Column(name = "pergunta_id", nullable = false)
    private UUID perguntaId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "alternativa_id", nullable = false)
    private UUID alternativaId;

    @Column(nullable = false)
    private Boolean correta;

    public RespostaQuiz(UUID usuarioId, AlternativaQuiz alternativa) {
        this.id = UUID.randomUUID();
        this.perguntaId = alternativa.getPergunta().getId();
        this.usuarioId = usuarioId;
        this.alternativaId = alternativa.getId();
        this.correta = alternativa.getCorreta();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof RespostaQuiz outro)) {
            return false;
        }

        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
