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
import com.readrace.api.exception.SoQuemSegueComentaException;
import com.readrace.api.model.Post;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.ComunidadeRepository;
import com.readrace.api.repository.CurtidaRepository;
import com.readrace.api.repository.MembroComunidadeRepository;
import com.readrace.api.repository.PostRepository;
import com.readrace.api.repository.UsuarioRepository;

/**
 * Comentários em thread de post de comunidade (#150).
 *
 * <p>Comentário e resposta são linhas em {@code post} com {@code post_pai_id} e sem escopo próprio:
 * a comunidade vem do post raiz da thread. Responder é comentar um comentário. Só threads de post
 * raiz de comunidade aceitam comentário; post de clube ou de livro respondem 404, como um post
 * inexistente, e também qualquer post da thread que esteja excluído.
 */
@Service
@Transactional(readOnly = true)
public class ComentarioService {

    private static final Comparator<Post> MAIS_ANTIGO_PRIMEIRO =
            Comparator.comparing(Post::getCriadoEm).thenComparing(Post::getId);

    private final PostRepository postRepository;
    private final ComunidadeRepository comunidadeRepository;
    private final MembroComunidadeRepository membroRepository;
    private final CurtidaRepository curtidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioAtual usuarioAtual;

    public ComentarioService(
            PostRepository postRepository,
            ComunidadeRepository comunidadeRepository,
            MembroComunidadeRepository membroRepository,
            CurtidaRepository curtidaRepository,
            UsuarioRepository usuarioRepository,
            UsuarioAtual usuarioAtual) {
        this.postRepository = postRepository;
        this.comunidadeRepository = comunidadeRepository;
        this.membroRepository = membroRepository;
        this.curtidaRepository = curtidaRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioAtual = usuarioAtual;
    }

    /**
     * A thread abaixo do post (ou do comentário) informado. Quem não segue também lê; {@code
     * podeComentar} diz se o campo aparece.
     */
    public ComentariosResponse listar(UUID postId) {
        Post raiz = raizComentavel(postId);
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        List<UUID> ids = postRepository.idsDaThread(postId, PostRepository.PROFUNDIDADE_MAXIMA);
        if (ids.isEmpty()) {
            return new ComentariosResponse(List.of(), segue(raiz.getComunidadeId(), usuarioId));
        }

        List<Post> comentarios = postRepository.findAllById(ids);
        Map<UUID, List<Post>> porPai =
                comentarios.stream().collect(Collectors.groupingBy(Post::getPostPaiId));
        Map<UUID, Usuario> autores =
                usuarioRepository
                        .findAllById(comentarios.stream().map(Post::getAutorId).distinct().toList())
                        .stream()
                        .collect(Collectors.toMap(Usuario::getId, Function.identity()));
        Map<UUID, Long> curtidas =
                curtidaRepository.contarPorPostIds(ids).stream()
                        .collect(
                                Collectors.toMap(
                                        CurtidaRepository.ContagemPorPost::getPostId,
                                        CurtidaRepository.ContagemPorPost::getTotal));
        Set<UUID> curtidosPorMim =
                Set.copyOf(curtidaRepository.postsCurtidosPorUsuario(usuarioId, ids));

        Arvore arvore = new Arvore(porPai, autores, curtidas, curtidosPorMim);
        return new ComentariosResponse(
                arvore.respostasDe(postId), segue(raiz.getComunidadeId(), usuarioId));
    }

    /** Com o id de um comentário, cria uma resposta a ele. */
    @Transactional
    public ComentariosResponse.Comentario comentar(UUID postId, String texto) {
        Post raiz = raizComentavel(postId);
        UUID autorId = usuarioAtual.idDoUsuarioAtual().valor();
        if (!segue(raiz.getComunidadeId(), autorId)) {
            throw new SoQuemSegueComentaException();
        }

        Post comentario = postRepository.save(Post.criarComentario(autorId, postId, texto));
        return paraComentario(
                comentario, usuarioRepository.findById(autorId).orElse(null), 0L, false, List.of());
    }

    /**
     * O post raiz da thread de {@code postId}, se a thread aceita comentário: nenhum post do
     * caminho excluído, a raiz é de comunidade e a comunidade não foi excluída.
     */
    private Post raizComentavel(UUID postId) {
        List<UUID> caminho = postRepository.idsAteARaiz(postId, PostRepository.PROFUNDIDADE_MAXIMA);
        Map<UUID, Post> posts =
                postRepository.findAllById(caminho).stream()
                        .collect(Collectors.toMap(Post::getId, Function.identity()));
        if (caminho.isEmpty()
                || caminho.stream()
                        .map(posts::get)
                        .anyMatch(post -> post == null || post.getExcluidoEm() != null)) {
            throw new PostNaoEncontradoException();
        }

        Post raiz = posts.get(caminho.getLast());
        boolean comunidadeAtiva =
                raiz.getPostPaiId() == null
                        && raiz.getComunidadeId() != null
                        && comunidadeRepository
                                .findById(raiz.getComunidadeId())
                                .filter(comunidade -> comunidade.getExcluidoEm() == null)
                                .isPresent();
        if (!comunidadeAtiva) {
            throw new PostNaoEncontradoException();
        }
        return raiz;
    }

    private boolean segue(UUID comunidadeId, UUID usuarioId) {
        return membroRepository
                .findByComunidade_IdAndUsuarioId(comunidadeId, usuarioId)
                .isPresent();
    }

    /** Monta a árvore a partir dos comentários agrupados pelo pai. */
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
                                            autores.get(comentario.getAutorId()),
                                            curtidas.getOrDefault(comentario.getId(), 0L),
                                            curtidosPorMim.contains(comentario.getId()),
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
