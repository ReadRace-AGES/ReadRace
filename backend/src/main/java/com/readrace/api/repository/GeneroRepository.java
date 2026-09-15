package com.readrace.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Genero;

public interface GeneroRepository extends JpaRepository<Genero, UUID> {

    @Query(
            value =
                    """
                    SELECT g.*
                    FROM genero g
                    JOIN livro_genero lg ON lg.genero_id = g.id
                    WHERE lg.livro_id = :livroId
                    LIMIT 1
                    """,
            nativeQuery = true)
    Optional<Genero> buscarPorLivroId(@Param("livroId") UUID livroId);
}
