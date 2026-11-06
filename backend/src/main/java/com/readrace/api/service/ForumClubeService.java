package com.readrace.api.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.ForumClubeResponse;
import com.readrace.api.exception.ClubeNaoEncontradoException;
import com.readrace.api.exception.RecursoNaoEncontradoException;
import com.readrace.api.exception.SoMembroPostaException;
import com.readrace.api.model.Cargo;
import com.readrace.api.model.ClubeDoLivro;
import com.readrace.api.model.Livro;
import com.readrace.api.model.Post;
import com.readrace.api.model.SequenciaDeLeitura;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.ClubeDoLivroRepository;
import com.readrace.api.repository.CurtidaRepository;
import com.readrace.api.repository.MembroClubeRepository;
import com.readrace.api.repository.PostRepository;
import com.readrace.api.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class ForumClubeService {

    private final ClubeDoLivroRepository clubeRepository;
    private final PostRepository postRepository;
    private final CurtidaRepository curtidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioAtual usuarioAtual;
    private final MembroClubeRepository membroClubeRepository;

    public ForumClubeService(
            ClubeDoLivroRepository clubeRepository,
            PostRepository postRepository,
            CurtidaRepository curtidaRepository,
            UsuarioRepository usuarioRepository,
            UsuarioAtual usuarioAtual,
            MembroClubeRepository membroClubeRepository) {
        this.clubeRepository = clubeRepository;
        this.postRepository = postRepository;
        this.curtidaRepository = curtidaRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioAtual = usuarioAtual;
        this.membroClubeRepository = membroClubeRepository;
    }

    /** Leitura pura: nada é criado, alterado ou apagado, e nenhuma curtida é gerada. */
    public ForumClubeResponse buscar(UUID clubeId) {
        ClubeDoLivro clube =
                clubeRepository
                        .findByIdAndExcluidoEmIsNull(clubeId)
                        .orElseThrow(
                                () -> new RecursoNaoEncontradoException("Clube não encontrado."));

        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();
        return new ForumClubeResponse(
                cabecalho(clube), posts(clubeId, usuarioId), podeEscrever(clubeId, usuarioId));
    }

    @Transactional(readOnly = false)
    public ForumClubeResponse.Post publicar(UUID clubeId, String texto) {
        clubeRepository
                .findByIdAndExcluidoEmIsNull(clubeId)
                .orElseThrow(ClubeNaoEncontradoException::new);
        UUID autorId = usuarioAtual.idDoUsuarioAtual().valor();
        if (!podeEscrever(clubeId, autorId)) {
            throw new SoMembroPostaException();
        }

        Post post = postRepository.save(Post.criarRaizDeClube(autorId, clubeId, texto));
        Usuario autor = usuarioRepository.findById(autorId).orElse(null);
        return paraPost(post, autor, 0L, false, SequenciaDeLeitura.hoje());
    }

    private boolean podeEscrever(UUID clubeId, UUID usuarioId) {
        return membroClubeRepository
                .findByClube_IdAndUsuarioId(clubeId, usuarioId)
                .map(
                        membro ->
                                membro.getCargoClube() == Cargo.MEMBRO
                                        || membro.getCargoClube() == Cargo.ADMINISTRADOR)
                .orElse(false);
    }

    private static ForumClubeResponse.Clube cabecalho(ClubeDoLivro clube) {
        Livro livro = clube.getLivro();
        String autor =
                livro.getLivroAutores().stream()
                        .map(vinculo -> vinculo.getAutor().getNome())
                        .collect(Collectors.joining(", "));

        return new ForumClubeResponse.Clube(
                clube.getId(),
                clube.getNome(),
                new ForumClubeResponse.LivroAtual(
                        livro.getTitulo(), autor.isEmpty() ? null : autor, livro.getCapaUrl()));
    }

    private List<ForumClubeResponse.Post> posts(UUID clubeId, UUID usuarioId) {
        List<Post> posts =
                postRepository
                        .findByClubeIdAndPostPaiIdIsNullAndExcluidoEmIsNullOrderByCriadoEmDesc(
                                clubeId);
        if (posts.isEmpty()) {
            return List.of();
        }

        List<UUID> postIds = posts.stream().map(Post::getId).toList();

        Map<UUID, Usuario> autores =
                usuarioRepository
                        .findAllById(posts.stream().map(Post::getAutorId).distinct().toList())
                        .stream()
                        .collect(Collectors.toMap(Usuario::getId, Function.identity()));

        Map<UUID, Long> curtidas =
                curtidaRepository.contarPorPostIds(postIds).stream()
                        .collect(
                                Collectors.toMap(
                                        CurtidaRepository.ContagemPorPost::getPostId,
                                        CurtidaRepository.ContagemPorPost::getTotal));

        Set<UUID> curtidosPorMim =
                Set.copyOf(curtidaRepository.postsCurtidosPorUsuario(usuarioId, postIds));
        LocalDate hoje = SequenciaDeLeitura.hoje();

        return posts.stream()
                .map(
                        post ->
                                paraPost(
                                        post,
                                        autores.get(post.getAutorId()),
                                        curtidas.getOrDefault(post.getId(), 0L),
                                        curtidosPorMim.contains(post.getId()),
                                        hoje))
                .toList();
    }

    private static ForumClubeResponse.Post paraPost(
            Post post, Usuario autor, long totalCurtidas, boolean curtidoPorMim, LocalDate hoje) {
        return new ForumClubeResponse.Post(
                post.getId(),
                new ForumClubeResponse.Autor(
                        post.getAutorId(),
                        autor == null ? null : autor.getNome(),
                        autor == null ? null : autor.getAvatarUrl(),
                        autor == null ? null : autor.sequenciaExibida(hoje)),
                post.getCriadoEm(),
                post.getConteudo(),
                totalCurtidas,
                curtidoPorMim);
    }
}
