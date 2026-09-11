package com.readrace.api.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Post;

public interface CurtidaRepository extends Repository<Post, UUID> {

    @Query(
            value =
                    """
                    SELECT COUNT(*)
                    FROM curtida
                    WHERE post_id = :postId
                    """,
            nativeQuery = true)
    long contarPorPostId(@Param("postId") UUID postId);
}
