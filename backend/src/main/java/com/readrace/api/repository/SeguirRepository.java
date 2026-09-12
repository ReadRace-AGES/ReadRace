package com.readrace.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Seguir;
import com.readrace.api.model.Usuario;

public interface SeguirRepository extends JpaRepository<Seguir, UUID> {

    @Query(
            """
            SELECT s.seguido
            FROM Seguir s
            WHERE s.seguidor.id = :usuarioId
              AND s.seguido.excluidoEm IS NULL
              AND EXISTS (
                  SELECT reciproco.id
                  FROM Seguir reciproco
                  WHERE reciproco.seguidor.id = s.seguido.id
                    AND reciproco.seguido.id = :usuarioId
              )
            ORDER BY LOWER(s.seguido.nomeUsuario)
            """)
    List<Usuario> buscarAmigos(@Param("usuarioId") UUID usuarioId);

    @Query(
            """
            SELECT s.seguido
            FROM Seguir s
            WHERE s.seguidor.id = :usuarioId
              AND s.seguido.excluidoEm IS NULL
              AND LOWER(s.seguido.nomeUsuario)
                  LIKE LOWER(CONCAT('%', :termo, '%'))
              AND EXISTS (
                  SELECT reciproco.id
                  FROM Seguir reciproco
                  WHERE reciproco.seguidor.id = s.seguido.id
                    AND reciproco.seguido.id = :usuarioId
              )
            ORDER BY LOWER(s.seguido.nomeUsuario)
            """)
    List<Usuario> buscarAmigosPorUsername(
            @Param("usuarioId") UUID usuarioId, @Param("termo") String termo);

    @Query(
            """
            SELECT COUNT(s)
            FROM Seguir s
            WHERE s.seguidor.id = :usuarioId
              AND s.seguido.id = :oponenteId
              AND EXISTS (
                  SELECT reciproco.id
                  FROM Seguir reciproco
                  WHERE reciproco.seguidor.id = :oponenteId
                    AND reciproco.seguido.id = :usuarioId
              )
            """)
    long contarAmizadeReciproca(
            @Param("usuarioId") UUID usuarioId, @Param("oponenteId") UUID oponenteId);
}
