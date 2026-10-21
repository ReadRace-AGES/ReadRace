package com.readrace.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Livro;

public interface LivroRepository extends JpaRepository<Livro, UUID> {

    @EntityGraph(attributePaths = {"livroAutores", "livroAutores.autor"})
    List<Livro> findByTituloContainingIgnoreCase(String termo);

    Optional<Livro> findByIsbn(String isbn);

    // O ISBN identifica o livro entre usuários (#155): a restrição única também serializa a
    // criação quando dois usuários adicionam o mesmo livro novo ao mesmo tempo.
    @Modifying
    @Query(
            value =
                    """
            INSERT INTO livro (id, isbn, titulo, total_paginas, capa_url)
            VALUES (:id, :isbn, :titulo, :totalPaginas, :capaUrl)
            ON CONFLICT (isbn) DO NOTHING
            """,
            nativeQuery = true)
    void criarSeAusente(
            @Param("id") UUID id,
            @Param("isbn") String isbn,
            @Param("titulo") String titulo,
            @Param("totalPaginas") Integer totalPaginas,
            @Param("capaUrl") String capaUrl);
}
