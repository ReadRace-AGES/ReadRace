package com.readrace.api.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.ProgressoDesafio;

public interface ProgressoDesafioRepository extends JpaRepository<ProgressoDesafio, UUID> {

    @Query(
            """
            SELECT progresso
            FROM ProgressoDesafio progresso
            JOIN FETCH progresso.usuario
            WHERE progresso.desafio.id IN :desafioIds
            """)
    List<ProgressoDesafio> buscarPorDesafios(@Param("desafioIds") Collection<UUID> desafioIds);
}
