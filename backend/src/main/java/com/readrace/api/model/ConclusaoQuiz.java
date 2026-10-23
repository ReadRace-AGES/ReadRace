package com.readrace.api.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Uma por usuário por quiz ({@code uq_conclusao_quiz_usuario}): é o que impede pagar XP duas vezes.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "conclusao_quiz")
public class ConclusaoQuiz {

    @Id private UUID id;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "xp_pago", nullable = false)
    private Integer xpPago;

    public ConclusaoQuiz(Quiz quiz, UUID usuarioId) {
        this.id = UUID.randomUUID();
        this.quizId = quiz.getId();
        this.usuarioId = usuarioId;
        this.xpPago = quiz.getRecompensaXp();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ConclusaoQuiz outro)) {
            return false;
        }

        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
