package com.readrace.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.readrace.api.model.Autor;

public interface AutorRepository extends JpaRepository<Autor, UUID> {

    Optional<Autor> findByNome(String nome);
}
