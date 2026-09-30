package com.readrace.api.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Conquista;

public interface ConquistaRepository extends JpaRepository<Conquista, UUID> {

    @Query(
            value =
                    """
                    SELECT c.*
                    FROM conquista c
                    WHERE c.criterio IN (:criterios)
                      AND NOT EXISTS (
                          SELECT 1
                          FROM conquista_usuario cu
                          WHERE cu.conquista_id = c.id
                            AND cu.usuario_id = :usuarioId
                      )
                    ORDER BY c.meta_valor ASC, c.id ASC
                    """,
            nativeQuery = true)
    List<Conquista> buscarPendentes(
            @Param("usuarioId") UUID usuarioId, @Param("criterios") Collection<String> criterios);

    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO conquista_usuario (id, conquista_id, usuario_id, obtida_em)
                    VALUES (:id, :conquistaId, :usuarioId, now())
                    ON CONFLICT (usuario_id, conquista_id) DO NOTHING
                    """,
            nativeQuery = true)
    int registrarSeAusente(
            @Param("id") UUID id,
            @Param("conquistaId") UUID conquistaId,
            @Param("usuarioId") UUID usuarioId);

    @Query(
            value =
                    """
                    SELECT count(*)
                    FROM item_biblioteca
                    WHERE usuario_id = :usuarioId
                      AND status_leitura = 'lido'
                    """,
            nativeQuery = true)
    long contarLivrosLidos(@Param("usuarioId") UUID usuarioId);

    @Query(
            value =
                    """
                    SELECT coalesce(sum(pagina_maxima), 0)
                    FROM item_biblioteca
                    WHERE usuario_id = :usuarioId
                    """,
            nativeQuery = true)
    long somarPaginasLidas(@Param("usuarioId") UUID usuarioId);

    @Query(
            value =
                    """
                    SELECT count(*)
                    FROM desafio_amigo
                    WHERE status = 'finalizado'
                      AND (criador_id = :usuarioId OR oponente_id = :usuarioId)
                    """,
            nativeQuery = true)
    long contarDesafiosFinalizados(@Param("usuarioId") UUID usuarioId);
}
