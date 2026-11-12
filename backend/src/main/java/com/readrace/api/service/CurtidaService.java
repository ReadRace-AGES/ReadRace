package com.readrace.api.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.CurtidaResponse;
import com.readrace.api.exception.PostNaoEncontradoException;
import com.readrace.api.exception.SoMembroPostaException;
import com.readrace.api.model.Post;
import com.readrace.api.repository.CurtidaRepository;
import com.readrace.api.repository.MembroClubeRepository;
import com.readrace.api.repository.PostRepository;

@Service
public class CurtidaService {

    private final CurtidaRepository curtidaRepository;
    private final PostRepository postRepository;
    private final MembroClubeRepository membroClubeRepository;
    private final UsuarioAtual usuarioAtual;

    public CurtidaService(
            CurtidaRepository curtidaRepository,
            PostRepository postRepository,
            MembroClubeRepository membroClubeRepository,
            UsuarioAtual usuarioAtual) {
        this.curtidaRepository = curtidaRepository;
        this.postRepository = postRepository;
        this.membroClubeRepository = membroClubeRepository;
        this.usuarioAtual = usuarioAtual;
    }

    /**
     * Idempotente: curtir de novo não cria uma segunda curtida nem falha. Post de clube só pode ser
     * curtido por membro (#224); post de livro ou comunidade segue aberto.
     */
    @Transactional
    public CurtidaResponse curtir(UUID postId) {
        Post post =
                postRepository
                        .findByIdAndExcluidoEmIsNull(postId)
                        .orElseThrow(PostNaoEncontradoException::new);
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();
        exigirMembroSeForDeClube(post, usuarioId);

        curtidaRepository.criarSeAusente(UUID.randomUUID(), postId, usuarioId);

        return new CurtidaResponse(true, curtidaRepository.contarPorPostId(postId));
    }

    /** Idempotente: descurtir um post nunca curtido não falha. */
    @Transactional
    public CurtidaResponse descurtir(UUID postId) {
        garantirQuePostExiste(postId);
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        curtidaRepository.removerSeExistente(postId, usuarioId);

        return new CurtidaResponse(false, curtidaRepository.contarPorPostId(postId));
    }

    private void exigirMembroSeForDeClube(Post post, UUID usuarioId) {
        UUID clubeId = post.getClubeId();
        if (clubeId != null
                && membroClubeRepository.findByClube_IdAndUsuarioId(clubeId, usuarioId).isEmpty()) {
            throw new SoMembroPostaException(
                    "Somente membros do clube podem curtir posts do fórum.");
        }
    }

    private void garantirQuePostExiste(UUID postId) {
        if (!postRepository.existsByIdAndExcluidoEmIsNull(postId)) {
            throw new PostNaoEncontradoException();
        }
    }
}
