package com.readrace.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.ClubeDoLivro;
import com.readrace.api.model.MembroClube;

public interface MembroClubeRepository extends JpaRepository<MembroClube, UUID> {

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
     * Ranking de um clube, do maior para o menor número de {@code Pontos}. Quem chama limita o
     * tamanho pelo {@code Pageable} — a Página do clube (#35) mostra 7.
     *
     * <p>O desempate por nome é escolha do backend, não regra de produto: o seed não tem empate,
     * mas sem uma segunda chave a ordem do banco seria arbitrária e a posição mudaria entre
     * chamadas.
     */
    @Query(
            "select u.id as usuarioId, u.nome as nome, u.avatarUrl as avatarUrl,"
                    + " mc.pontos as pontos"
                    + " from MembroClube mc, Usuario u"
                    + " where mc.clube.id = :clubeId"
                    + " and u.id = mc.usuarioId and u.excluidoEm is null"
                    + " order by mc.pontos desc, u.nome asc")
    List<LinhaRankingClube> rankingDoClube(@Param("clubeId") UUID clubeId, Pageable limite);
}
