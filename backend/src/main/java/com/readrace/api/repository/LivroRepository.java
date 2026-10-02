package com.readrace.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.readrace.api.model.Livro;

public interface LivroRepository extends JpaRepository<Livro, UUID> {

    @EntityGraph(attributePaths = {"livroAutores", "livroAutores.autor"})
    List<Livro> findByTituloContainingIgnoreCase(String termo);
}
