package com.readrace.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Curtida;

public interface CurtidaRepository extends Repository<Curtida, UUID> {

    interface ContagemPorPost {
        UUID getPostId();

        long getTotal();
    }

    @Query(
            value =
                    "SELECT post_id AS postId, COUNT(*) AS total FROM curtida WHERE post_id IN (:postIds) GROUP BY post_id",
            nativeQuery = true)
    List<ContagemPorPost> contarPorPostIds(@Param("postIds") List<UUID> postIds);

    @Query(value = "SELECT COUNT(*) FROM curtida WHERE post_id = :postId", nativeQuery = true)
    long contarPorPostId(@Param("postId") UUID postId);

    /**
     * ids dos posts, dentre os informados, já curtidos pelo usuário — para o campo curtidoPorMim.
     */
    @Query(
            value =
                    "SELECT post_id FROM curtida WHERE usuario_id = :usuarioId AND post_id IN (:postIds)",
            nativeQuery = true)
    List<UUID> postsCurtidosPorUsuario(
            @Param("usuarioId") UUID usuarioId, @Param("postIds") List<UUID> postIds);

    // A restrição única (post_id, usuario_id) também serializa curtidas concorrentes do mesmo par.
    @Modifying
    @Query(
            value =
                    """
            INSERT INTO curtida (id, post_id, usuario_id)
            VALUES (:id, :postId, :usuarioId)
            ON CONFLICT (post_id, usuario_id) DO NOTHING
            """,
            nativeQuery = true)
    void criarSeAusente(
            @Param("id") UUID id, @Param("postId") UUID postId, @Param("usuarioId") UUID usuarioId);

    @Modifying
    @Query(
            value = "DELETE FROM curtida WHERE post_id = :postId AND usuario_id = :usuarioId",
            nativeQuery = true)
    void removerSeExistente(@Param("postId") UUID postId, @Param("usuarioId") UUID usuarioId);
}
