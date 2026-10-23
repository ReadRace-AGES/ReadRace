package com.readrace.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Seguir;
import com.readrace.api.model.Usuario;

public interface SeguirRepository extends JpaRepository<Seguir, UUID> {

    boolean existsBySeguidorIdAndSeguidoId(UUID seguidorId, UUID seguidoId);

    @Query(
            "select count(s) from Seguir s where s.seguido.id = :seguidoId and s.seguidor.excluidoEm is null")
    long countBySeguidoId(@Param("seguidoId") UUID seguidoId);

    @Query(
            "select s.seguidor from Seguir s where s.seguido.id = :usuarioId"
                    + " and s.seguidor.excluidoEm is null order by s.criadoEm desc, s.id asc")
    List<Usuario> buscarSeguidores(@Param("usuarioId") UUID usuarioId);

    @Query(
            "select s.seguido from Seguir s where s.seguidor.id = :usuarioId"
                    + " and s.seguido.excluidoEm is null order by s.criadoEm desc, s.id asc")
    List<Usuario> buscarSeguindo(@Param("usuarioId") UUID usuarioId);

    @Modifying
    @Query(
            value =
                    """
            INSERT INTO seguir (id, seguidor_id, seguido_id)
            VALUES (:id, :seguidorId, :seguidoId)
            ON CONFLICT (seguidor_id, seguido_id) DO NOTHING
            """,
            nativeQuery = true)
    void criarSeAusente(
            @Param("id") UUID id,
            @Param("seguidorId") UUID seguidorId,
            @Param("seguidoId") UUID seguidoId);

    @Modifying
    @Query(
            value =
                    "DELETE FROM seguir WHERE seguidor_id = :seguidorId AND seguido_id = :seguidoId",
            nativeQuery = true)
    void removerSeExistente(
            @Param("seguidorId") UUID seguidorId, @Param("seguidoId") UUID seguidoId);

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
