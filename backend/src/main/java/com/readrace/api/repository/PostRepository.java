package com.readrace.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.readrace.api.model.Post;

public interface PostRepository extends JpaRepository<Post, UUID> {

    List<Post> findByLivroIdAndPostPaiIdIsNullAndExcluidoEmIsNullOrderByCriadoEmDesc(UUID livroId);

    /**
     * Posts raiz de um clube, do mais recente para o mais antigo (#36).
     *
     * <p>Comentário é post com pai e não entra na listagem; post de comunidade tem {@code clube_id}
     * nulo e por isso também fica de fora.
     */
    List<Post> findByClubeIdAndPostPaiIdIsNullAndExcluidoEmIsNullOrderByCriadoEmDesc(UUID clubeId);
}
