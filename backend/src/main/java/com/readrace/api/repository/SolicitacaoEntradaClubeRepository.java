package com.readrace.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.SolicitacaoEntradaClube;

public interface SolicitacaoEntradaClubeRepository
        extends JpaRepository<SolicitacaoEntradaClube, UUID> {
    @Query(
            value =
                    """
            select s.* from solicitacao_entrada_clube s
            where s.clube_id = :clubeId and s.usuario_id = :usuarioId
              and s.situacao = 'pendente'
            """,
            nativeQuery = true)
    Optional<SolicitacaoEntradaClube> buscarPendente(
            @Param("clubeId") UUID clubeId, @Param("usuarioId") UUID usuarioId);

    @Query(
            value =
                    """
            select s.* from solicitacao_entrada_clube s
            where s.clube_id = :clubeId
              and s.situacao = 'pendente'
            order by s.criada_em, s.id
            """,
            nativeQuery = true)
    List<SolicitacaoEntradaClube> listarPendentes(@Param("clubeId") UUID clubeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitacaoEntradaClube s where s.id = :id and s.clubeId = :clubeId")
    Optional<SolicitacaoEntradaClube> buscarComLock(
            @Param("id") UUID id, @Param("clubeId") UUID clubeId);

    @Query(
            value =
                    """
            select count(*) from solicitacao_entrada_clube s
            where s.clube_id = :clubeId
              and s.situacao = 'pendente'
            """,
            nativeQuery = true)
    long contarPendentes(@Param("clubeId") UUID clubeId);
}
