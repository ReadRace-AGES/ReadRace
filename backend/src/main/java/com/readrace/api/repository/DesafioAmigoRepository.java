package com.readrace.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.DesafioAmigo;
import com.readrace.api.model.StatusDesafio;

public interface DesafioAmigoRepository extends JpaRepository<DesafioAmigo, UUID> {

    @Query(
            """
            SELECT DISTINCT desafio
            FROM DesafioAmigo desafio
            JOIN FETCH desafio.criador
            JOIN FETCH desafio.oponente
            LEFT JOIN FETCH desafio.livro livro
            LEFT JOIN FETCH livro.livroAutores livroAutor
            LEFT JOIN FETCH livroAutor.autor
            WHERE (
                desafio.criador.id = :usuarioId
                OR desafio.oponente.id = :usuarioId
            )
              AND desafio.status <> :statusExcluido
            """)
    List<DesafioAmigo> buscarDoUsuario(
            @Param("usuarioId") UUID usuarioId,
            @Param("statusExcluido") StatusDesafio statusExcluido);

    @Query(
            """
            SELECT DISTINCT desafio
            FROM DesafioAmigo desafio
            JOIN FETCH desafio.criador
            JOIN FETCH desafio.oponente
            LEFT JOIN FETCH desafio.livro livro
            LEFT JOIN FETCH livro.livroAutores livroAutor
            LEFT JOIN FETCH livroAutor.autor
            WHERE desafio.id = :desafioId
              AND (
                  desafio.criador.id = :usuarioId
                  OR desafio.oponente.id = :usuarioId
              )
              AND desafio.status <> :statusExcluido
            """)
    Optional<DesafioAmigo> buscarPorIdEUsuario(
            @Param("desafioId") UUID desafioId,
            @Param("usuarioId") UUID usuarioId,
            @Param("statusExcluido") StatusDesafio statusExcluido);
}
