package com.readrace.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.readrace.api.model.Post;

public interface PostRepository extends JpaRepository<Post, UUID> {

    List<Post> findByLivroIdAndPostPaiIdIsNullAndExcluidoEmIsNullOrderByCriadoEmDesc(UUID livroId);
}