package com.readrace.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.readrace.api.model.ClubeDoLivro;

public interface ClubeDoLivroRepository extends JpaRepository<ClubeDoLivro, UUID> {

    Optional<ClubeDoLivro> findByIdAndExcluidoEmIsNull(UUID id);
}
