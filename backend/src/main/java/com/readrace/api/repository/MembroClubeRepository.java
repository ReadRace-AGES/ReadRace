package com.readrace.api.repository;

import java.util.List;
import java.util.UUID;

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
}
