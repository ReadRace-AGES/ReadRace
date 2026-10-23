package com.readrace.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.PerguntaQuiz;

public interface PerguntaQuizRepository extends JpaRepository<PerguntaQuiz, UUID> {

    Optional<PerguntaQuiz> findByIdAndQuizId(UUID id, UUID quizId);

    long countByQuizId(UUID quizId);

    /** Perguntas que o usuário ainda não respondeu, pela ordem. Quem chama limita a uma. */
    @Query(
            """
            SELECT pergunta
            FROM PerguntaQuiz pergunta
            WHERE pergunta.quizId = :quizId
              AND NOT EXISTS (
                SELECT 1
                FROM RespostaQuiz resposta
                WHERE resposta.perguntaId = pergunta.id
                  AND resposta.usuarioId = :usuarioId)
            ORDER BY pergunta.ordem
            """)
    List<PerguntaQuiz> naoRespondidas(
            @Param("quizId") UUID quizId, @Param("usuarioId") UUID usuarioId, Pageable limite);
}
