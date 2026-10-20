package com.readrace.api.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.ComunidadeResponse;
import com.readrace.api.dto.response.MembroComunidadeResponse;
import com.readrace.api.exception.ComunidadeNaoEncontradaException;
import com.readrace.api.exception.UnicoAdministradorException;
import com.readrace.api.model.Cargo;
import com.readrace.api.model.Comunidade;
import com.readrace.api.model.MembroComunidade;
import com.readrace.api.model.Post;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.ComunidadeRepository;
import com.readrace.api.repository.CurtidaRepository;
import com.readrace.api.repository.MembroComunidadeRepository;
import com.readrace.api.repository.PostRepository;
import com.readrace.api.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class ComunidadeService {

    private final ComunidadeRepository comunidadeRepository;
    private final MembroComunidadeRepository membroRepository;
    private final PostRepository postRepository;
    private final CurtidaRepository curtidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioAtual usuarioAtual;

    public ComunidadeService(
            ComunidadeRepository comunidadeRepository,
            MembroComunidadeRepository membroRepository,
            PostRepository postRepository,
            CurtidaRepository curtidaRepository,
            UsuarioRepository usuarioRepository,
            UsuarioAtual usuarioAtual) {
        this.comunidadeRepository = comunidadeRepository;
        this.membroRepository = membroRepository;
        this.postRepository = postRepository;
        this.curtidaRepository = curtidaRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioAtual = usuarioAtual;
    }

    public ComunidadeResponse buscar(UUID comunidadeId) {
        Comunidade comunidade = comunidade(comunidadeId);
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();
        List<Post> posts =
                postRepository
                        .findByComunidadeIdAndPostPaiIdIsNullAndExcluidoEmIsNullOrderByCriadoEmDesc(
                                comunidadeId);
        List<UUID> postIds = posts.stream().map(Post::getId).toList();
        Map<UUID, Usuario> autores =
                usuarioRepository
                        .findAllById(posts.stream().map(Post::getAutorId).distinct().toList())
                        .stream()
                        .collect(Collectors.toMap(Usuario::getId, Function.identity()));
        Map<UUID, Long> curtidas =
                postIds.isEmpty()
                        ? Map.of()
                        : curtidaRepository.contarPorPostIds(postIds).stream()
                                .collect(
                                        Collectors.toMap(
                                                CurtidaRepository.ContagemPorPost::getPostId,
                                                CurtidaRepository.ContagemPorPost::getTotal));
        Set<UUID> curtidosPorMim =
                postIds.isEmpty()
                        ? Set.of()
                        : Set.copyOf(curtidaRepository.postsCurtidosPorUsuario(usuarioId, postIds));
        Map<UUID, Long> comentarios =
                postIds.isEmpty()
                        ? Map.of()
                        : postRepository.contarComentariosPorPostIds(postIds).stream()
                                .collect(
                                        Collectors.toMap(
                                                PostRepository.ContagemComentarios::getPostId,
                                                PostRepository.ContagemComentarios::getTotal));

        return new ComunidadeResponse(
                comunidade.getId(),
                comunidade.getNome(),
                comunidade.getImagemUrl(),
                membroRepository.contarPorComunidade(comunidadeId),
                membroRepository
                        .findByComunidade_IdAndUsuarioId(comunidadeId, usuarioId)
                        .isPresent(),
                posts.stream()
                        .map(
                                post -> {
                                    Usuario autor = autores.get(post.getAutorId());
                                    return new ComunidadeResponse.Post(
                                            post.getId(),
                                            new ComunidadeResponse.Autor(
                                                    post.getAutorId(),
                                                    autor == null || autor.getNome() == null
                                                            ? ""
                                                            : autor.getNome(),
                                                    autor == null ? null : autor.getAvatarUrl()),
                                            post.getCriadoEm(),
                                            post.getConteudo(),
                                            curtidas.getOrDefault(post.getId(), 0L),
                                            curtidosPorMim.contains(post.getId()),
                                            comentarios.getOrDefault(post.getId(), 0L));
                                })
                        .toList());
    }

    @Transactional
    public MembroComunidadeResponse seguir(UUID comunidadeId) {
        Comunidade comunidade = comunidadeComLock(comunidadeId);
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();
        membroRepository.inserirSeAusente(UUID.randomUUID(), comunidade.getId(), usuarioId);
        return estado(comunidadeId, usuarioId);
    }

    @Transactional
    public MembroComunidadeResponse deixarDeSeguir(UUID comunidadeId) {
        // Serializa as alterações de participação antes de contar administradores.
        // Bloquear apenas o membro permite que dois administradores saiam simultaneamente.
        comunidadeComLock(comunidadeId);
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();
        MembroComunidade membro =
                membroRepository.buscarComLock(comunidadeId, usuarioId).orElse(null);
        if (membro != null
                && membro.getCargoComunidade() == Cargo.ADMINISTRADOR
                && membroRepository.contarAdministradores(comunidadeId) == 1) {
            throw new UnicoAdministradorException();
        }
        membroRepository.remover(comunidadeId, usuarioId);
        return estado(comunidadeId, usuarioId);
    }

    private MembroComunidadeResponse estado(UUID comunidadeId, UUID usuarioId) {
        return new MembroComunidadeResponse(
                membroRepository.contarPorComunidade(comunidadeId),
                membroRepository
                        .findByComunidade_IdAndUsuarioId(comunidadeId, usuarioId)
                        .isPresent());
    }

    private Comunidade comunidade(UUID comunidadeId) {
        return comunidadeRepository
                .findById(comunidadeId)
                .filter(comunidade -> comunidade.getExcluidoEm() == null)
                .orElseThrow(ComunidadeNaoEncontradaException::new);
    }

    private Comunidade comunidadeComLock(UUID comunidadeId) {
        return comunidadeRepository
                .buscarComLock(comunidadeId)
                .orElseThrow(ComunidadeNaoEncontradaException::new);
    }
}
