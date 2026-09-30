package com.readrace.api.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.CurtidaResponse;
import com.readrace.api.exception.PostNaoEncontradoException;
import com.readrace.api.repository.CurtidaRepository;
import com.readrace.api.repository.PostRepository;

@Service
public class CurtidaService {

    private final CurtidaRepository curtidaRepository;
    private final PostRepository postRepository;
    private final UsuarioAtualDeSeed usuarioAtual;

    public CurtidaService(
            CurtidaRepository curtidaRepository,
            PostRepository postRepository,
            UsuarioAtualDeSeed usuarioAtual) {
        this.curtidaRepository = curtidaRepository;
        this.postRepository = postRepository;
        this.usuarioAtual = usuarioAtual;
    }

    /** Idempotente: curtir de novo não cria uma segunda curtida nem falha. */
    @Transactional
    public CurtidaResponse curtir(UUID postId) {
        garantirQuePostExiste(postId);
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

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

    private void garantirQuePostExiste(UUID postId) {
        if (!postRepository.existsByIdAndExcluidoEmIsNull(postId)) {
            throw new PostNaoEncontradoException();
        }
    }
}
