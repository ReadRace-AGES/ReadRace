package com.readrace.api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.RespostaQuiz;

public interface RespostaQuizRepository extends JpaRepository<RespostaQuiz, UUID> {

    boolean existsByPerguntaIdAndUsuarioId(UUID perguntaId, UUID usuarioId);

    @Query(
            """
            SELECT count(resposta)
            FROM RespostaQuiz resposta, PerguntaQuiz pergunta
            WHERE pergunta.id = resposta.perguntaId
              AND pergunta.quizId = :quizId
              AND resposta.usuarioId = :usuarioId
            """)
    long contarRespondidas(@Param("quizId") UUID quizId, @Param("usuarioId") UUID usuarioId);

    @Query(
            """
            SELECT count(resposta)
            FROM RespostaQuiz resposta, PerguntaQuiz pergunta
            WHERE pergunta.id = resposta.perguntaId
              AND pergunta.quizId = :quizId
              AND resposta.usuarioId = :usuarioId
              AND resposta.correta = true
            """)
    long contarAcertos(@Param("quizId") UUID quizId, @Param("usuarioId") UUID usuarioId);
}
