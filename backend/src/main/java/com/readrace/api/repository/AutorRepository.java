package com.readrace.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Autor;

public interface AutorRepository extends JpaRepository<Autor, UUID> {

    @Query(
            value =
                    """
                    SELECT a.*
                    FROM autor a
                    JOIN livro_autor la ON la.autor_id = a.id
                    WHERE la.livro_id = :livroId
                    ORDER BY la.ordem
                    LIMIT 1
                    """,
            nativeQuery = true)
    Optional<Autor> buscarPrincipalPorLivroId(@Param("livroId") UUID livroId);
}
