package com.readrace.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.readrace.api.model.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    /** No máximo um, garantido por {@code uq_quiz_clube_ativo}. */
    Optional<Quiz> findByClubeIdAndAtivoTrue(UUID clubeId);
}
