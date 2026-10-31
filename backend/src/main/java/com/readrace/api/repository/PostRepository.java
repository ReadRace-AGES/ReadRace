package com.readrace.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Post;

public interface PostRepository extends JpaRepository<Post, UUID> {

    boolean existsByIdAndExcluidoEmIsNull(UUID id);

    Optional<Post> findByIdAndExcluidoEmIsNull(UUID id);

    List<Post> findByLivroIdAndPostPaiIdIsNullAndExcluidoEmIsNullOrderByCriadoEmDesc(UUID livroId);

    /**
     * Posts raiz de um clube, do mais recente para o mais antigo (#36).
     *
     * <p>Comentário é post com pai e não entra na listagem; post de comunidade tem {@code clube_id}
     * nulo e por isso também fica de fora.
     */
    List<Post> findByClubeIdAndPostPaiIdIsNullAndExcluidoEmIsNullOrderByCriadoEmDesc(UUID clubeId);

    List<Post> findByComunidadeIdAndPostPaiIdIsNullAndExcluidoEmIsNullOrderByCriadoEmDesc(
            UUID comunidadeId);

    /**
     * ids da thread abaixo do post (comentários e respostas, em qualquer nível), sem os excluídos.
     * Um comentário excluído leva junto as respostas dele (#150).
     */
    @Query(
            value =
                    """
                    WITH RECURSIVE thread AS (
                        SELECT id, 1 AS nivel FROM post
                        WHERE post_pai_id = :postId AND excluido_em IS NULL
                        UNION ALL
                        SELECT p.id, t.nivel + 1 FROM post p
                        JOIN thread t ON p.post_pai_id = t.id
                        WHERE p.excluido_em IS NULL AND t.nivel < :profundidadeMaxima
                    )
                    SELECT id FROM thread
                    """,
            nativeQuery = true)
    List<UUID> idsDaThread(
            @Param("postId") UUID postId, @Param("profundidadeMaxima") int profundidadeMaxima);

    /** ids do post e dos pais dele até o post raiz, nessa ordem (#150). */
    @Query(
            value =
                    """
                    WITH RECURSIVE acima AS (
                        SELECT id, post_pai_id, 0 AS nivel FROM post WHERE id = :postId
                        UNION ALL
                        SELECT p.id, p.post_pai_id, a.nivel + 1 FROM post p
                        JOIN acima a ON p.id = a.post_pai_id
                        WHERE a.nivel < :profundidadeMaxima
                    )
                    SELECT id FROM acima ORDER BY nivel
                    """,
            nativeQuery = true)
    List<UUID> idsAteARaiz(
            @Param("postId") UUID postId, @Param("profundidadeMaxima") int profundidadeMaxima);

    /** Total da thread de cada post: comentários e respostas em qualquer nível, sem excluídos. */
    @Query(
            value =
                    """
                    WITH RECURSIVE thread AS (
                        SELECT id, post_pai_id AS raiz, 1 AS nivel FROM post
                        WHERE post_pai_id IN (:postIds) AND excluido_em IS NULL
                        UNION ALL
                        SELECT p.id, t.raiz, t.nivel + 1 FROM post p
                        JOIN thread t ON p.post_pai_id = t.id
                        WHERE p.excluido_em IS NULL AND t.nivel < :profundidadeMaxima
                    )
                    SELECT raiz AS postId, count(*) AS total FROM thread GROUP BY raiz
                    """,
            nativeQuery = true)
    List<ContagemComentarios> contarComentariosDaThread(
            @Param("postIds") List<UUID> postIds,
            @Param("profundidadeMaxima") int profundidadeMaxima);

    /**
     * Limite das consultas recursivas. O banco não impede um ciclo de pais longo; a API só cria
     * resposta com id novo, mas o limite garante que a recursão termina.
     */
    int PROFUNDIDADE_MAXIMA = 100;

    default List<ContagemComentarios> contarComentariosPorPostIds(List<UUID> postIds) {
        return contarComentariosDaThread(postIds, PROFUNDIDADE_MAXIMA);
    }

    interface ContagemComentarios {
        UUID getPostId();

        long getTotal();
    }
}
