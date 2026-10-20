package com.readrace.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Comunidade;
import com.readrace.api.model.MembroComunidade;

public interface MembroComunidadeRepository extends JpaRepository<MembroComunidade, UUID> {

    @Query(
            "select mc.comunidade from MembroComunidade mc"
                    + " where mc.usuarioId = :usuarioId and mc.comunidade.excluidoEm is null"
                    + " order by mc.comunidade.nome")
    List<Comunidade> comunidadesDoUsuario(@Param("usuarioId") UUID usuarioId);

    Optional<MembroComunidade> findByComunidade_IdAndUsuarioId(UUID comunidadeId, UUID usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select mc from MembroComunidade mc"
                    + " where mc.comunidade.id = :comunidadeId and mc.usuarioId = :usuarioId")
    Optional<MembroComunidade> buscarComLock(
            @Param("comunidadeId") UUID comunidadeId, @Param("usuarioId") UUID usuarioId);

    @Query("select count(mc) from MembroComunidade mc where mc.comunidade.id = :comunidadeId")
    long contarPorComunidade(@Param("comunidadeId") UUID comunidadeId);

    @Query(
            value =
                    "SELECT count(*) FROM membro_comunidade"
                            + " WHERE comunidade_id = :comunidadeId AND cargo_comunidade = 'administrador'",
            nativeQuery = true)
    long contarAdministradores(@Param("comunidadeId") UUID comunidadeId);

    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO membro_comunidade (id, comunidade_id, usuario_id, cargo_comunidade)
                    VALUES (:id, :comunidadeId, :usuarioId, 'membro')
                    ON CONFLICT (comunidade_id, usuario_id) DO NOTHING
                    """,
            nativeQuery = true)
    void inserirSeAusente(
            @Param("id") UUID id,
            @Param("comunidadeId") UUID comunidadeId,
            @Param("usuarioId") UUID usuarioId);

    @Modifying
    @Query(
            "delete from MembroComunidade mc"
                    + " where mc.comunidade.id = :comunidadeId and mc.usuarioId = :usuarioId")
    void remover(@Param("comunidadeId") UUID comunidadeId, @Param("usuarioId") UUID usuarioId);

    @Query(
            "select mc.comunidade.id as id, count(mc) as total from MembroComunidade mc"
                    + " where mc.comunidade.excluidoEm is null"
                    + " group by mc.comunidade.id")
    List<ContagemMembros> contarMembrosPorComunidade();
}
