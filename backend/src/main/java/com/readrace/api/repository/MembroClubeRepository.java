
package com.readrace.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Cargo;
import com.readrace.api.model.ClubeDoLivro;
import com.readrace.api.model.MembroClube;

public interface MembroClubeRepository extends JpaRepository<MembroClube, UUID> {

    Optional<MembroClube> findByClube_IdAndUsuarioId(
            UUID clubeId,
            UUID usuarioId);

    @Query(
            "select mc.clube from MembroClube mc"
                    + " where mc.usuarioId = :usuarioId and mc.clube.excluidoEm is null"
                    + " order by mc.clube.nome")
    List<ClubeDoLivro> clubesDoUsuario(@Param("usuarioId") UUID usuarioId);

    @Query(
            "select mc.clube.id as id, count(mc) as total from MembroClube mc"
                    + " where mc.clube.excluidoEm is null"
                    + " group by mc.clube.id")
    List<ContagemMembros> contarMembrosPorClube();

    /**
     * Ranking de um clube, do maior para o menor número de Pontos.
     * A Página do clube mostra até 7 posições.
     */
    @Query(
            "select u.id as usuarioId, u.nome as nome, u.avatarUrl as avatarUrl,"
                    + " mc.pontos as pontos"
                    + " from MembroClube mc, Usuario u"
                    + " where mc.clube.id = :clubeId"
                    + " and u.id = mc.usuarioId and u.excluidoEm is null"
                    + " order by mc.pontos desc, u.nome asc")
    List<LinhaRankingClube> rankingDoClube(
            @Param("clubeId") UUID clubeId,
            Pageable limite);

    // Task #152 - Sair de um clube do livro

    /**
     * Conta todos os membros de um clube.
     */
    long countByClube_Id(UUID clubeId);

    /**
     * Conta os membros de um clube que possuem determinado cargo.
     */
    long countByClube_IdAndCargoClube(
            UUID clubeId,
            Cargo cargoClube);
}
