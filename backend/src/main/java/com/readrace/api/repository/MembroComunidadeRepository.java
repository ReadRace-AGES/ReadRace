package com.readrace.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query(
            "select mc.comunidade.id as id, count(mc) as total from MembroComunidade mc"
                    + " where mc.comunidade.excluidoEm is null"
                    + " group by mc.comunidade.id")
    List<ContagemMembros> contarMembrosPorComunidade();
}
