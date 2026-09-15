package com.readrace.api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.LivroDetalheResponse;
import com.readrace.api.exception.LivroNaoEncontradoException;
import com.readrace.api.model.Genero;
import com.readrace.api.model.ItemBiblioteca;
import com.readrace.api.model.Livro;
import com.readrace.api.model.Post;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.CurtidaRepository;
import com.readrace.api.repository.GeneroRepository;
import com.readrace.api.repository.ItemBibliotecaRepository;
import com.readrace.api.repository.LivroRepository;
import com.readrace.api.repository.PostRepository;
import com.readrace.api.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class LivroService {

    private final LivroRepository livroRepository;
    private final GeneroRepository generoRepository;
    private final ItemBibliotecaRepository itemBibliotecaRepository;
    private final PostRepository postRepository;
    private final UsuarioRepository usuarioRepository;
    private final CurtidaRepository curtidaRepository;
    private final UsuarioAtualDeSeed usuarioAtual;

    public LivroService(
            LivroRepository livroRepository,
            GeneroRepository generoRepository,
            ItemBibliotecaRepository itemBibliotecaRepository,
            PostRepository postRepository,
            UsuarioRepository usuarioRepository,
            CurtidaRepository curtidaRepository,
            UsuarioAtualDeSeed usuarioAtual) {

        this.livroRepository = livroRepository;
        this.generoRepository = generoRepository;
        this.itemBibliotecaRepository = itemBibliotecaRepository;
        this.postRepository = postRepository;
        this.usuarioRepository = usuarioRepository;
        this.curtidaRepository = curtidaRepository;
        this.usuarioAtual = usuarioAtual;
    }

    public LivroDetalheResponse buscarDetalhe(UUID livroId) {

        Livro livro =
                livroRepository.findById(livroId).orElseThrow(LivroNaoEncontradoException::new);

        String autor =
                livro.getLivroAutores().stream()
                        .map(vinculo -> vinculo.getAutor().getNome())
                        .collect(java.util.stream.Collectors.joining(", "));

        Genero genero = generoRepository.buscarPorLivroId(livroId).orElse(null);

        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        LivroDetalheResponse.Progresso progresso = buscarProgresso(usuarioId, livro);

        List<LivroDetalheResponse.Post> posts = buscarPosts(livroId);

        LivroDetalheResponse.Livro livroResponse =
                new LivroDetalheResponse.Livro(
                        livro.getId(),
                        livro.getTitulo(),
                        autor.isEmpty() ? null : autor,
                        livro.getCapaUrl(),
                        genero != null ? genero.getNome() : null,
                        livro.getTotalPaginas());

        return new LivroDetalheResponse(livroResponse, progresso, posts);
    }

    private LivroDetalheResponse.Progresso buscarProgresso(UUID usuarioId, Livro livro) {

        return itemBibliotecaRepository
                .findByUsuarioIdAndLivroId(usuarioId, livro.getId())
                .map(item -> criarProgresso(item, livro))
                .orElse(null);
    }

    private LivroDetalheResponse.Progresso criarProgresso(ItemBiblioteca item, Livro livro) {

        int percentual = Math.round((item.getPaginaAtual() * 100f) / livro.getTotalPaginas());

        boolean concluido = item.estaConcluido();

        return new LivroDetalheResponse.Progresso(
                item.getPaginaAtual(), item.getPaginaMaxima(), percentual, concluido);
    }

    private List<LivroDetalheResponse.Post> buscarPosts(UUID livroId) {

        return postRepository
                .findByLivroIdAndPostPaiIdIsNullAndExcluidoEmIsNullOrderByCriadoEmDesc(livroId)
                .stream()
                .map(this::criarPostResponse)
                .toList();
    }

    private LivroDetalheResponse.Post criarPostResponse(Post post) {

        Usuario autor =
                usuarioRepository
                        .findById(post.getAutorId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Autor do post não encontrado: "
                                                        + post.getAutorId()));

        LivroDetalheResponse.Autor autorResponse =
                new LivroDetalheResponse.Autor(
                        autor.getNome(), autor.getAvatarUrl(), autor.getDiasConsecutivos());

        long curtidas = curtidaRepository.contarPorPostId(post.getId());

        return new LivroDetalheResponse.Post(
                post.getId(), autorResponse, post.getConteudo(), post.getCriadoEm(), curtidas);
    }
}
