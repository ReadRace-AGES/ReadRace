package com.readrace.api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.readrace.api.model.ConclusaoQuiz;

public interface ConclusaoQuizRepository extends JpaRepository<ConclusaoQuiz, UUID> {}
