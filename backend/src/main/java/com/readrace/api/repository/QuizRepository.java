package com.readrace.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    /** Busca o quiz ativo do clube. */
    Optional<Quiz> findByClubeIdAndAtivoTrue(UUID clubeId);

    /**
     * Desativa o quiz ativo anterior sem apagá-lo nem remover seu histórico.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Quiz quiz
            SET quiz.ativo = false
            WHERE quiz.clubeId = :clubeId
              AND quiz.ativo = true
            """)
    int desativarAtivoDoClube(@Param("clubeId") UUID clubeId);
}
