
package com.readrace.api.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.ComentariosResponse;
import com.readrace.api.exception.PostNaoEncontradoException;
import com.readrace.api.exception.SoMembroPostaException;
import com.readrace.api.exception.SoQuemSegueComentaException;
import com.readrace.api.model.Post;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.ClubeDoLivroRepository;
import com.readrace.api.repository.ComunidadeRepository;
import com.readrace.api.repository.CurtidaRepository;
import com.readrace.api.repository.MembroClubeRepository;
import com.readrace.api.repository.MembroComunidadeRepository;
import com.readrace.api.repository.PostRepository;
import com.readrace.api.repository.UsuarioRepository;

/**
 * Comentarios em threads de posts de comunidade, clube e livro (#236).
 *
 * Comentarios e respostas sao posts com post_pai_id.
 * O escopo e determinado pelo post raiz.
 */
@Service
@Transactional(readOnly = true)
public class ComentarioService {

    private static final Comparator<Post> MAIS_ANTIGO_PRIMEIRO =
            Comparator.comparing(Post::getCriadoEm).thenComparing(Post::getId);

    private final PostRepository postRepository;
    private final ComunidadeRepository comunidadeRepository;
    private final ClubeDoLivroRepository clubeRepository;
    private final MembroComunidadeRepository membroRepository;
    private final MembroClubeRepository membroClubeRepository;
    private final CurtidaRepository curtidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioAtual usuarioAtual;

    public ComentarioService(
            PostRepository postRepository,
            ComunidadeRepository comunidadeRepository,
            ClubeDoLivroRepository clubeRepository,
            MembroComunidadeRepository membroRepository,
            MembroClubeRepository membroClubeRepository,
            CurtidaRepository curtidaRepository,
            UsuarioRepository usuarioRepository,
            UsuarioAtual usuarioAtual) {
        this.postRepository = postRepository;
        this.comunidadeRepository = comunidadeRepository;
        this.clubeRepository = clubeRepository;
        this.membroRepository = membroRepository;
        this.membroClubeRepository = membroClubeRepository;
        this.curtidaRepository = curtidaRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioAtual = usuarioAtual;
    }

    /**
     * Lista a thread de comentarios de um post ou comentario.
     * A leitura e permitida mesmo quando o usuario nao pode escrever.
     */
    public ComentariosResponse listar(UUID postId) {
        Post raiz = raizComentavel(postId);
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        boolean podeComentar = podeComentar(raiz, usuarioId);

        List<UUID> ids =
                postRepository.idsDaThread(
                        postId, PostRepository.PROFUNDIDADE_MAXIMA);

        if (ids.isEmpty()) {
            return new ComentariosResponse(List.of(), podeComentar);
        }

        List<Post> comentarios = postRepository.findAllById(ids);

        Map<UUID, List<Post>> porPai =
                comentarios.stream()
                        .collect(Collectors.groupingBy(Post::getPostPaiId));

        Map<UUID, Usuario> autores =
                usuarioRepository
                        .findAllById(
                                comentarios.stream()
                                        .map(Post::getAutorId)
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Usuario::getId,
                                        Function.identity()));

        Map<UUID, Long> curtidas =
                curtidaRepository.contarPorPostIds(ids).stream()
                        .collect(
                                Collectors.toMap(
                                        CurtidaRepository.ContagemPorPost::getPostId,
                                        CurtidaRepository.ContagemPorPost::getTotal));

        Set<UUID> curtidosPorMim =
                Set.copyOf(
                        curtidaRepository.postsCurtidosPorUsuario(
                                usuarioId, ids));

        Arvore arvore =
                new Arvore(porPai, autores, curtidas, curtidosPorMim);

        return new ComentariosResponse(
                arvore.respostasDe(postId), podeComentar);
    }

    /**
     * Cria um comentario ou uma resposta.
     * A permissao e determinada pelo post raiz da thread.
     */
    @Transactional
    public ComentariosResponse.Comentario comentar(
            UUID postId, String texto) {

        Post raiz = raizComentavel(postId);
        UUID autorId = usuarioAtual.idDoUsuarioAtual().valor();

        validarPermissao(raiz, autorId);

        Post comentario =
                postRepository.save(
                        Post.criarComentario(autorId, postId, texto));

        return paraComentario(
                comentario,
                usuarioRepository.findById(autorId).orElse(null),
                0L,
                false,
                List.of());
    }

    /**
     * Localiza a raiz da thread e valida o escopo.
     */
    private Post raizComentavel(UUID postId) {
        List<UUID> caminho =
                postRepository.idsAteARaiz(
                        postId, PostRepository.PROFUNDIDADE_MAXIMA);

        Map<UUID, Post> posts =
                postRepository.findAllById(caminho).stream()
                        .collect(
                                Collectors.toMap(
                                        Post::getId,
                                        Function.identity()));

        if (caminho.isEmpty()
                || caminho.stream()
                        .map(posts::get)
                        .anyMatch(
                                post ->
                                        post == null
                                                || post.getExcluidoEm() != null)) {
            throw new PostNaoEncontradoException();
        }

        Post raiz = posts.get(caminho.getLast());

        if (raiz.getPostPaiId() != null) {
            throw new PostNaoEncontradoException();
        }

        if (raiz.getComunidadeId() != null) {
            boolean comunidadeAtiva =
                    comunidadeRepository
                            .findById(raiz.getComunidadeId())
                            .filter(
                                    comunidade ->
                                            comunidade.getExcluidoEm() == null)
                            .isPresent();

            if (!comunidadeAtiva) {
                throw new PostNaoEncontradoException();
            }

            return raiz;
        }

        if (raiz.getClubeId() != null) {
            boolean clubeAtivo =
                    clubeRepository
                            .findByIdAndExcluidoEmIsNull(raiz.getClubeId())
                            .isPresent();

            if (!clubeAtivo) {
                throw new PostNaoEncontradoException();
            }

            return raiz;
        }

        if (raiz.getLivroId() != null) {
            return raiz;
        }

        throw new PostNaoEncontradoException();
    }

    /**
     * Verifica se o usuario pode comentar no escopo do post.
     */
    private boolean podeComentar(Post raiz, UUID usuarioId) {
        if (raiz.getComunidadeId() != null) {
            return segue(raiz.getComunidadeId(), usuarioId);
        }

        if (raiz.getClubeId() != null) {
            return membroClubeRepository
                    .findByClube_IdAndUsuarioId(
                            raiz.getClubeId(), usuarioId)
                    .isPresent();
        }

        return raiz.getLivroId() != null;
    }

    /**
     * Mantem as regras de permissao especificas de cada escopo.
     */
    private void validarPermissao(Post raiz, UUID usuarioId) {
        if (podeComentar(raiz, usuarioId)) {
            return;
        }

        if (raiz.getClubeId() != null) {
            throw new SoMembroPostaException();
        }

        throw new SoQuemSegueComentaException();
    }

    private boolean segue(UUID comunidadeId, UUID usuarioId) {
        return membroRepository
                .findByComunidade_IdAndUsuarioId(
                        comunidadeId, usuarioId)
                .isPresent();
    }

    /**
     * Monta a arvore de comentarios e respostas.
     */
    private record Arvore(
            Map<UUID, List<Post>> porPai,
            Map<UUID, Usuario> autores,
            Map<UUID, Long> curtidas,
            Set<UUID> curtidosPorMim) {

        List<ComentariosResponse.Comentario> respostasDe(UUID paiId) {
            return porPai.getOrDefault(paiId, List.of()).stream()
                    .sorted(MAIS_ANTIGO_PRIMEIRO)
                    .map(
                            comentario ->
                                    paraComentario(
                                            comentario,
                                            autores.get(
                                                    comentario.getAutorId()),
                                            curtidas.getOrDefault(
                                                    comentario.getId(), 0L),
                                            curtidosPorMim.contains(
                                                    comentario.getId()),
                                            respostasDe(comentario.getId())))
                    .toList();
        }
    }

    private static ComentariosResponse.Comentario paraComentario(
            Post comentario,
            Usuario autor,
            long totalCurtidas,
            boolean curtidoPorMim,
            List<ComentariosResponse.Comentario> respostas) {

        return new ComentariosResponse.Comentario(
                comentario.getId(),
                comentario.getConteudo(),
                comentario.getCriadoEm(),
                new ComentariosResponse.Autor(
                        comentario.getAutorId(),
                        autor == null ? null : autor.getNome(),
                        autor == null ? null : autor.getAvatarUrl()),
                totalCurtidas,
                curtidoPorMim,
                respostas);
    }
}
