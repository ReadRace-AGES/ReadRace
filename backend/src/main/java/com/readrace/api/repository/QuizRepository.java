package com.readrace.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    /** No máximo um, garantido por {@code uq_quiz_clube_ativo}. */
    Optional<Quiz> findByClubeIdAndAtivoTrue(UUID clubeId);

    boolean existsByClubeIdAndAtivoTrue(UUID clubeId);

    /**
     * Executa na hora, antes do INSERT do quiz novo. Um {@code UPDATE} por entidade só sairia no
     * flush, depois do INSERT, e {@code uq_quiz_clube_ativo} recusaria dois ativos juntos.
     */
    @Modifying(flushAutomatically = true)
    @Query(
            "UPDATE Quiz quiz SET quiz.ativo = false WHERE quiz.clubeId = :clubeId AND quiz.ativo = true")
    int desativarAtivoDoClube(@Param("clubeId") UUID clubeId);
}
