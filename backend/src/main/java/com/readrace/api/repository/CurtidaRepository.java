package com.readrace.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Post;

public interface CurtidaRepository extends Repository<Post, UUID> {

    interface ContagemPorPost {
        UUID getPostId();

        long getTotal();
    }

    @Query(
            value =
                    "SELECT post_id AS postId, COUNT(*) AS total FROM curtida WHERE post_id IN (:postIds) GROUP BY post_id",
            nativeQuery = true)
    List<ContagemPorPost> contarPorPostIds(@Param("postIds") List<UUID> postIds);
}
