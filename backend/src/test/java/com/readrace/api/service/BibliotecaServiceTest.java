package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.readrace.api.dto.response.BibliotecaResponse;
import com.readrace.api.dto.response.LivroBibliotecaResponse;
import com.readrace.api.model.Autor;
import com.readrace.api.model.ItemBiblioteca;
import com.readrace.api.model.Livro;
import com.readrace.api.model.LivroAutor;
import com.readrace.api.model.StatusLeitura;
import com.readrace.api.repository.ItemBibliotecaRepository;
import com.readrace.api.repository.ItemBibliotecaRepository.UltimaLeitura;

@DisplayName("BibliotecaService")
class BibliotecaServiceTest {
    private static final UUID USUARIO = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-09-11T12:00:00Z");

    private ItemBibliotecaRepository repositorio;
    private BibliotecaService service;

    @BeforeEach
    void setUp() {
        repositorio = mock(ItemBibliotecaRepository.class);
        service = new BibliotecaService(repositorio, new UsuarioAtualDeSeed(USUARIO));
        when(repositorio.buscarUltimaLeituraPorItem(anyCollection())).thenReturn(List.of());
    }

    @Test
    void deve_repartir_a_biblioteca_do_usuario_atual_por_estado() {
        when(repositorio.findByUsuarioId(USUARIO))
                .thenReturn(
                        List.of(
                                item("Lendo", StatusLeitura.lendo, false, AGORA),
                                item("Desejo", StatusLeitura.desejo, false, AGORA),
                                item("Lido", StatusLeitura.lido, false, AGORA)));

        BibliotecaResponse resposta = service.buscar();

        assertThat(titulos(resposta.lendo())).containsExactly("Lendo");
        assertThat(titulos(resposta.desejo())).containsExactly("Desejo");
        assertThat(titulos(resposta.lidos())).containsExactly("Lido");
        assertThat(resposta.favoritos()).isEmpty();
    }

    @Test
    void favorito_deve_aparecer_nos_favoritos_e_tambem_na_lista_do_seu_estado() {
        when(repositorio.findByUsuarioId(USUARIO))
                .thenReturn(
                        List.of(
                                item("Favorito em leitura", StatusLeitura.lendo, true, AGORA),
                                item("Só em leitura", StatusLeitura.lendo, false, AGORA)));

        BibliotecaResponse resposta = service.buscar();

        assertThat(titulos(resposta.favoritos())).containsExactly("Favorito em leitura");
        assertThat(titulos(resposta.lendo()))
                .containsExactlyInAnyOrder("Favorito em leitura", "Só em leitura");
    }

    @Test
    void deve_devolver_quatro_listas_vazias_quando_a_biblioteca_estiver_vazia() {
        when(repositorio.findByUsuarioId(USUARIO)).thenReturn(List.of());

        BibliotecaResponse resposta = service.buscar();

        assertThat(resposta.favoritos()).isEmpty();
        assertThat(resposta.lendo()).isEmpty();
        assertThat(resposta.desejo()).isEmpty();
        assertThat(resposta.lidos()).isEmpty();
        verify(repositorio, never()).buscarUltimaLeituraPorItem(anyCollection());
    }

    @Test
    void deve_ordenar_pelo_ultimo_registro_de_leitura_quando_ele_for_mais_recente() {
        ItemBiblioteca adicionadoOntem =
                item("Adicionado ontem", StatusLeitura.lendo, false, AGORA.minusDays(1));
        ItemBiblioteca lidoHoje =
                item("Lido hoje", StatusLeitura.lendo, false, AGORA.minusDays(30));
        when(repositorio.findByUsuarioId(USUARIO)).thenReturn(List.of(adicionadoOntem, lidoHoje));
        when(repositorio.buscarUltimaLeituraPorItem(anyCollection()))
                .thenReturn(List.of(leitura(lidoHoje, AGORA)));

        BibliotecaResponse resposta = service.buscar();

        assertThat(titulos(resposta.lendo())).containsExactly("Lido hoje", "Adicionado ontem");
    }

    @Test
    void deve_manter_a_data_de_adicao_quando_o_registro_for_mais_antigo_que_ela() {
        ItemBiblioteca readicionado =
                item("Readicionado", StatusLeitura.lido, false, AGORA.minusDays(1));
        ItemBiblioteca outro = item("Outro", StatusLeitura.lido, false, AGORA.minusDays(2));
        when(repositorio.findByUsuarioId(USUARIO)).thenReturn(List.of(outro, readicionado));
        when(repositorio.buscarUltimaLeituraPorItem(anyCollection()))
                .thenReturn(List.of(leitura(readicionado, AGORA.minusDays(10))));

        BibliotecaResponse resposta = service.buscar();

        assertThat(titulos(resposta.lidos())).containsExactly("Readicionado", "Outro");
    }

    @Test
    void deve_juntar_os_autores_na_ordem_do_vinculo_e_deixar_nulo_sem_autor() {
        Livro semAutor = livro("Anônimo");
        Livro doisAutores = livro("Dupla");
        vincularAutor(doisAutores, "Primeiro");
        vincularAutor(doisAutores, "Segundo");
        when(repositorio.findByUsuarioId(USUARIO))
                .thenReturn(
                        List.of(
                                item(doisAutores, StatusLeitura.desejo, false, AGORA),
                                item(semAutor, StatusLeitura.desejo, false, AGORA.minusDays(1))));

        BibliotecaResponse resposta = service.buscar();

        assertThat(resposta.desejo())
                .extracting(LivroBibliotecaResponse::autor)
                .containsExactly("Primeiro, Segundo", null);
    }

    private static List<String> titulos(List<LivroBibliotecaResponse> livros) {
        return livros.stream().map(LivroBibliotecaResponse::titulo).toList();
    }

    // As entidades só têm construtor protegido (o Flyway e o seed são donos dos dados), então os
    // testes montam os objetos por reflexão, como o Hibernate faria.
    private static ItemBiblioteca item(
            String titulo, StatusLeitura status, boolean favorito, OffsetDateTime adicionadoEm) {
        return item(livro(titulo), status, favorito, adicionadoEm);
    }

    private static ItemBiblioteca item(
            Livro livro, StatusLeitura status, boolean favorito, OffsetDateTime adicionadoEm) {
        ItemBiblioteca item = instanciar(ItemBiblioteca.class);
        ReflectionTestUtils.setField(item, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(item, "usuarioId", USUARIO);
        ReflectionTestUtils.setField(item, "livro", livro);
        ReflectionTestUtils.setField(item, "statusLeitura", status);
        ReflectionTestUtils.setField(item, "favorito", favorito);
        ReflectionTestUtils.setField(item, "paginaAtual", 0);
        ReflectionTestUtils.setField(item, "paginaMaxima", 0);
        ReflectionTestUtils.setField(item, "adicionadoEm", adicionadoEm);
        return item;
    }

    private static Livro livro(String titulo) {
        Livro livro = instanciar(Livro.class);
        ReflectionTestUtils.setField(livro, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(livro, "titulo", titulo);
        ReflectionTestUtils.setField(livro, "totalPaginas", 100);
        ReflectionTestUtils.setField(livro, "capaUrl", "https://capa/" + titulo);
        ReflectionTestUtils.setField(livro, "livroAutores", new ArrayList<LivroAutor>());
        return livro;
    }

    private static void vincularAutor(Livro livro, String nome) {
        Autor autor = instanciar(Autor.class);
        ReflectionTestUtils.setField(autor, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(autor, "nome", nome);
        LivroAutor vinculo = instanciar(LivroAutor.class);
        ReflectionTestUtils.setField(vinculo, "livro", livro);
        ReflectionTestUtils.setField(vinculo, "autor", autor);
        livro.getLivroAutores().add(vinculo);
    }

    private static UltimaLeitura leitura(ItemBiblioteca item, OffsetDateTime quando) {
        return new UltimaLeitura() {
            @Override
            public UUID getItemId() {
                return item.getId();
            }

            @Override
            public Instant getRegistradoEm() {
                return quando.toInstant();
            }
        };
    }

    private static <T> T instanciar(Class<T> tipo) {
        try {
            var construtor = tipo.getDeclaredConstructor();
            construtor.setAccessible(true);
            return construtor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
