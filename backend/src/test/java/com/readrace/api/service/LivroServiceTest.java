package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.readrace.api.dto.response.LivroDetalheResponse;
import com.readrace.api.exception.LivroNaoEncontradoException;
import com.readrace.api.model.Autor;
import com.readrace.api.model.Genero;
import com.readrace.api.model.ItemBiblioteca;
import com.readrace.api.model.Livro;
import com.readrace.api.model.Post;
import com.readrace.api.model.Usuario;
import com.readrace.api.model.UsuarioId;
import com.readrace.api.repository.AutorRepository;
import com.readrace.api.repository.CurtidaRepository;
import com.readrace.api.repository.GeneroRepository;
import com.readrace.api.repository.ItemBibliotecaRepository;
import com.readrace.api.repository.LivroRepository;
import com.readrace.api.repository.PostRepository;
import com.readrace.api.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LivroService")
class LivroServiceTest {

    @Mock private LivroRepository livroRepository;
    @Mock private AutorRepository autorRepository;
    @Mock private GeneroRepository generoRepository;
    @Mock private ItemBibliotecaRepository itemBibliotecaRepository;
    @Mock private PostRepository postRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CurtidaRepository curtidaRepository;
    @Mock private UsuarioAtualDeSeed usuarioAtual;

    @InjectMocks private LivroService livroService;

    @Test
    void deve_lancar_excecao_quando_livro_nao_existir() {
        UUID livroId = UUID.randomUUID();

        when(livroRepository.findById(livroId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> livroService.buscarDetalhe(livroId))
                .isInstanceOf(LivroNaoEncontradoException.class);
    }

    @Test
    void deve_buscar_detalhes_do_livro_sem_progresso_e_sem_posts() {
        UUID livroId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        Livro livro = org.mockito.Mockito.mock(Livro.class);
        Autor autor = org.mockito.Mockito.mock(Autor.class);
        Genero genero = org.mockito.Mockito.mock(Genero.class);

        when(livro.getId()).thenReturn(livroId);
        when(livro.getTitulo()).thenReturn("Dom Casmurro");
        when(livro.getCapaUrl()).thenReturn("capa.jpg");
        when(livro.getTotalPaginas()).thenReturn(300);

        when(autor.getNome()).thenReturn("Machado de Assis");
        when(genero.getNome()).thenReturn("Romance");

        when(livroRepository.findById(livroId)).thenReturn(Optional.of(livro));
        when(autorRepository.buscarPrincipalPorLivroId(livroId)).thenReturn(Optional.of(autor));
        when(generoRepository.buscarPorLivroId(livroId)).thenReturn(Optional.of(genero));

        when(usuarioAtual.idDoUsuarioAtual()).thenReturn(new UsuarioId(usuarioId));

        when(itemBibliotecaRepository.findByUsuarioIdAndLivroId(usuarioId, livroId))
                .thenReturn(Optional.empty());

        when(postRepository.findByLivroIdAndPostPaiIdIsNullAndExcluidoEmIsNullOrderByCriadoEmDesc(
                        livroId))
                .thenReturn(List.of());

        LivroDetalheResponse resultado = livroService.buscarDetalhe(livroId);

        assertThat(resultado.livro().id()).isEqualTo(livroId);
        assertThat(resultado.livro().titulo()).isEqualTo("Dom Casmurro");
        assertThat(resultado.livro().autor()).isEqualTo("Machado de Assis");
        assertThat(resultado.livro().capaUrl()).isEqualTo("capa.jpg");
        assertThat(resultado.livro().genero()).isEqualTo("Romance");
        assertThat(resultado.livro().totalPaginas()).isEqualTo(300);

        assertThat(resultado.progresso()).isNull();
        assertThat(resultado.posts()).isEmpty();
    }

    @Test
    void deve_retornar_autor_e_genero_nulos_quando_nao_existirem() {
        UUID livroId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        Livro livro = org.mockito.Mockito.mock(Livro.class);

        when(livro.getId()).thenReturn(livroId);
        when(livro.getTitulo()).thenReturn("Livro sem autor");
        when(livro.getTotalPaginas()).thenReturn(100);

        when(livroRepository.findById(livroId)).thenReturn(Optional.of(livro));
        when(autorRepository.buscarPrincipalPorLivroId(livroId)).thenReturn(Optional.empty());
        when(generoRepository.buscarPorLivroId(livroId)).thenReturn(Optional.empty());

        when(usuarioAtual.idDoUsuarioAtual()).thenReturn(new UsuarioId(usuarioId));

        when(itemBibliotecaRepository.findByUsuarioIdAndLivroId(usuarioId, livroId))
                .thenReturn(Optional.empty());

        when(postRepository.findByLivroIdAndPostPaiIdIsNullAndExcluidoEmIsNullOrderByCriadoEmDesc(
                        livroId))
                .thenReturn(List.of());

        LivroDetalheResponse resultado = livroService.buscarDetalhe(livroId);

        assertThat(resultado.livro().autor()).isNull();
        assertThat(resultado.livro().genero()).isNull();
    }

    @Test
    void deve_calcular_progresso_do_usuario() {
        UUID livroId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        Livro livro = org.mockito.Mockito.mock(Livro.class);
        ItemBiblioteca item = org.mockito.Mockito.mock(ItemBiblioteca.class);

        when(livro.getId()).thenReturn(livroId);
        when(livro.getTotalPaginas()).thenReturn(200);

        when(item.getPaginaAtual()).thenReturn(50);
        when(item.getPaginaMaxima()).thenReturn(80);

        when(livroRepository.findById(livroId)).thenReturn(Optional.of(livro));
        when(autorRepository.buscarPrincipalPorLivroId(livroId)).thenReturn(Optional.empty());
        when(generoRepository.buscarPorLivroId(livroId)).thenReturn(Optional.empty());

        when(usuarioAtual.idDoUsuarioAtual()).thenReturn(new UsuarioId(usuarioId));

        when(itemBibliotecaRepository.findByUsuarioIdAndLivroId(usuarioId, livroId))
                .thenReturn(Optional.of(item));

        when(postRepository.findByLivroIdAndPostPaiIdIsNullAndExcluidoEmIsNullOrderByCriadoEmDesc(
                        livroId))
                .thenReturn(List.of());

        LivroDetalheResponse resultado = livroService.buscarDetalhe(livroId);

        assertThat(resultado.progresso()).isNotNull();
        assertThat(resultado.progresso().paginaAtual()).isEqualTo(50);
        // assertThat(resultado.progresso().paginaMaxima()).isEqualTo(80);
        assertThat(resultado.progresso().percentual()).isEqualTo(25);
        assertThat(resultado.progresso().concluido()).isFalse();
    }

    @Test
    void deve_marcar_livro_como_concluido() {
        UUID livroId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        Livro livro = org.mockito.Mockito.mock(Livro.class);
        ItemBiblioteca item = org.mockito.Mockito.mock(ItemBiblioteca.class);

        when(livro.getId()).thenReturn(livroId);
        when(livro.getTotalPaginas()).thenReturn(200);

        when(item.getPaginaAtual()).thenReturn(200);
        when(item.getPaginaMaxima()).thenReturn(200);

        when(livroRepository.findById(livroId)).thenReturn(Optional.of(livro));
        when(autorRepository.buscarPrincipalPorLivroId(livroId)).thenReturn(Optional.empty());
        when(generoRepository.buscarPorLivroId(livroId)).thenReturn(Optional.empty());

        when(usuarioAtual.idDoUsuarioAtual()).thenReturn(new UsuarioId(usuarioId));

        when(itemBibliotecaRepository.findByUsuarioIdAndLivroId(usuarioId, livroId))
                .thenReturn(Optional.of(item));

        when(postRepository.findByLivroIdAndPostPaiIdIsNullAndExcluidoEmIsNullOrderByCriadoEmDesc(
                        livroId))
                .thenReturn(List.of());

        LivroDetalheResponse resultado = livroService.buscarDetalhe(livroId);

        assertThat(resultado.progresso().percentual()).isEqualTo(100);
        assertThat(resultado.progresso().concluido()).isTrue();
    }

    @Test
    void deve_converter_posts_e_contar_curtidas() {
        UUID livroId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();

        OffsetDateTime criadoEm = OffsetDateTime.now();

        Livro livro = org.mockito.Mockito.mock(Livro.class);
        Post post = org.mockito.Mockito.mock(Post.class);
        Usuario autorPost = org.mockito.Mockito.mock(Usuario.class);

        when(livro.getId()).thenReturn(livroId);
        when(livro.getTotalPaginas()).thenReturn(300);

        when(post.getId()).thenReturn(postId);
        when(post.getAutorId()).thenReturn(autorId);
        when(post.getConteudo()).thenReturn("Excelente livro!");
        when(post.getCriadoEm()).thenReturn(criadoEm);

        when(autorPost.getNome()).thenReturn("Leitor");
        when(autorPost.getAvatarUrl()).thenReturn("avatar.jpg");
        when(autorPost.getDiasConsecutivos()).thenReturn(10);

        when(livroRepository.findById(livroId)).thenReturn(Optional.of(livro));
        when(autorRepository.buscarPrincipalPorLivroId(livroId)).thenReturn(Optional.empty());
        when(generoRepository.buscarPorLivroId(livroId)).thenReturn(Optional.empty());

        when(usuarioAtual.idDoUsuarioAtual()).thenReturn(new UsuarioId(usuarioId));

        when(itemBibliotecaRepository.findByUsuarioIdAndLivroId(usuarioId, livroId))
                .thenReturn(Optional.empty());

        when(postRepository.findByLivroIdAndPostPaiIdIsNullAndExcluidoEmIsNullOrderByCriadoEmDesc(
                        livroId))
                .thenReturn(List.of(post));

        when(usuarioRepository.findById(autorId)).thenReturn(Optional.of(autorPost));
        when(curtidaRepository.contarPorPostId(postId)).thenReturn(7L);

        LivroDetalheResponse resultado = livroService.buscarDetalhe(livroId);

        assertThat(resultado.posts()).hasSize(1);

        LivroDetalheResponse.Post resultadoPost = resultado.posts().getFirst();

        assertThat(resultadoPost.id()).isEqualTo(postId);
        assertThat(resultadoPost.texto()).isEqualTo("Excelente livro!");
        assertThat(resultadoPost.criadoEm()).isEqualTo(criadoEm);
        assertThat(resultadoPost.curtidas()).isEqualTo(7);

        assertThat(resultadoPost.autor().nome()).isEqualTo("Leitor");
        assertThat(resultadoPost.autor().avatarUrl()).isEqualTo("avatar.jpg");
        assertThat(resultadoPost.autor().sequenciaDias()).isEqualTo(10);
    }

    @Test
    void deve_lancar_excecao_quando_autor_do_post_nao_existir() {
        UUID livroId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID autorId = UUID.randomUUID();

        Livro livro = org.mockito.Mockito.mock(Livro.class);
        Post post = org.mockito.Mockito.mock(Post.class);

        when(livro.getId()).thenReturn(livroId);

        when(post.getAutorId()).thenReturn(autorId);

        when(livroRepository.findById(livroId)).thenReturn(Optional.of(livro));

        when(livroRepository.findById(livroId)).thenReturn(Optional.of(livro));
        when(autorRepository.buscarPrincipalPorLivroId(livroId)).thenReturn(Optional.empty());
        when(generoRepository.buscarPorLivroId(livroId)).thenReturn(Optional.empty());

        when(usuarioAtual.idDoUsuarioAtual()).thenReturn(new UsuarioId(usuarioId));

        when(itemBibliotecaRepository.findByUsuarioIdAndLivroId(usuarioId, livroId))
                .thenReturn(Optional.empty());

        when(postRepository.findByLivroIdAndPostPaiIdIsNullAndExcluidoEmIsNullOrderByCriadoEmDesc(
                        livroId))
                .thenReturn(List.of(post));

        when(usuarioRepository.findById(autorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> livroService.buscarDetalhe(livroId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(autorId.toString());
    }
}
