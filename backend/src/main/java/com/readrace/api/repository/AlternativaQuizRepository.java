package com.readrace.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.readrace.api.model.AlternativaQuiz;

public interface AlternativaQuizRepository extends JpaRepository<AlternativaQuiz, UUID> {

    Optional<AlternativaQuiz> findByIdAndPergunta_Id(UUID id, UUID perguntaId);
}
