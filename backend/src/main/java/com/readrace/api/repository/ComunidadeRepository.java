package com.readrace.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Comunidade;

public interface ComunidadeRepository extends JpaRepository<Comunidade, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Comunidade c where c.id = :id and c.excluidoEm is null")
    Optional<Comunidade> buscarComLock(@Param("id") UUID id);

    List<Comunidade> findByNomeContainingIgnoreCaseAndExcluidoEmIsNull(String nome);

    @Query(
            value =
                    """
                    SELECT c.* FROM comunidade c
                    WHERE c.excluido_em IS NULL
                      AND NOT EXISTS (SELECT 1 FROM membro_comunidade mc
                                      WHERE mc.comunidade_id = c.id AND mc.usuario_id = :usuarioId)
                    ORDER BY c.nome, c.id
                    LIMIT 5
                    """,
            nativeQuery = true)
    List<Comunidade> sugeridasParaUsuario(@Param("usuarioId") UUID usuarioId);
}
