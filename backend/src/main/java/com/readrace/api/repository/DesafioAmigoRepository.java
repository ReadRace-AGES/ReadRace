package com.readrace.api.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.DesafioAmigo;
import com.readrace.api.model.StatusDesafio;

public interface DesafioAmigoRepository extends JpaRepository<DesafioAmigo, UUID> {

    @Query(
            """
            SELECT desafio
            FROM DesafioAmigo desafio
            JOIN FETCH desafio.criador
            JOIN FETCH desafio.oponente
            LEFT JOIN FETCH desafio.livro
            WHERE (
                desafio.criador.id = :usuarioId
                OR desafio.oponente.id = :usuarioId
            )
              AND desafio.status <> :statusExcluido
            ORDER BY desafio.inicioEm DESC, desafio.id DESC
            """)
    List<DesafioAmigo> buscarPrimeiraPaginaDoUsuario(
            @Param("usuarioId") UUID usuarioId,
            @Param("statusExcluido") StatusDesafio statusExcluido,
            Pageable pageable);

    @Query(
            """
            SELECT desafio
            FROM DesafioAmigo desafio
            JOIN FETCH desafio.criador
            JOIN FETCH desafio.oponente
            LEFT JOIN FETCH desafio.livro
            WHERE (
                desafio.criador.id = :usuarioId
                OR desafio.oponente.id = :usuarioId
            )
              AND desafio.status <> :statusExcluido
              AND (
                  desafio.inicioEm < :cursorInicioEm
                  OR (
                      desafio.inicioEm = :cursorInicioEm
                      AND desafio.id < :cursorId
                  )
              )
            ORDER BY desafio.inicioEm DESC, desafio.id DESC
            """)
    List<DesafioAmigo> buscarPaginaDoUsuarioApos(
            @Param("usuarioId") UUID usuarioId,
            @Param("statusExcluido") StatusDesafio statusExcluido,
            @Param("cursorInicioEm") OffsetDateTime cursorInicioEm,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);

    @Query(
            """
            SELECT DISTINCT desafio
            FROM DesafioAmigo desafio
            LEFT JOIN FETCH desafio.livro livro
            LEFT JOIN FETCH livro.livroAutores livroAutor
            LEFT JOIN FETCH livroAutor.autor
            WHERE desafio.id IN :desafioIds
            """)
    List<DesafioAmigo> carregarAutoresPorIds(@Param("desafioIds") Collection<UUID> desafioIds);

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
