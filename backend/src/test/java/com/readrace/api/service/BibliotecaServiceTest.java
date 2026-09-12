package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.readrace.api.dto.response.BibliotecaResponse;
import com.readrace.api.dto.response.LivroBibliotecaResponse;
import com.readrace.api.dto.response.PaginaBibliotecaResponse;
import com.readrace.api.exception.ParametroInvalidoException;
import com.readrace.api.exception.RecursoNaoEncontradoException;
import com.readrace.api.model.Autor;
import com.readrace.api.model.CursorBiblioteca;
import com.readrace.api.model.ItemBiblioteca;
import com.readrace.api.model.Livro;
import com.readrace.api.model.LivroAutor;
import com.readrace.api.model.StatusLeitura;
import com.readrace.api.repository.ItemBibliotecaRepository;
import com.readrace.api.repository.ItemBibliotecaRepository.ItemPaginado;

@DisplayName("BibliotecaService")
class BibliotecaServiceTest {
    private static final UUID USUARIO = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant AGORA = Instant.parse("2026-09-11T12:00:00Z");

    private ItemBibliotecaRepository repositorio;
    private BibliotecaService service;

    @BeforeEach
    void setUp() {
        repositorio = mock(ItemBibliotecaRepository.class);
        service = new BibliotecaService(repositorio, new UsuarioAtualDeSeed(USUARIO));
        when(repositorio.paginarFavoritos(any(), any(), any(), anyInt())).thenReturn(List.of());
        when(repositorio.paginarPorStatus(any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of());
        // Devolve as entidades dos ids pedidos, na ordem inversa, para provar que a ordem da
        // resposta vem da página e não do banco.
        when(repositorio.findByIdIn(anyCollection()))
                .thenAnswer(
                        chamada -> {
                            Collection<UUID> ids = chamada.getArgument(0);
                            List<ItemBiblioteca> itens =
                                    ids.stream().map(BibliotecaServiceTest::itemComId).toList();
                            return itens.reversed();
                        });
    }

    @Test
    void deve_pedir_a_primeira_pagina_de_cada_lista_com_o_limite_padrao_mais_um() {
        service.buscar(null);

        int esperado = BibliotecaService.LIMITE_PADRAO + 1;
        Instant inicio = CursorBiblioteca.INICIO.atividade();
        verify(repositorio)
                .paginarFavoritos(USUARIO, inicio, CursorBiblioteca.INICIO.itemId(), esperado);
        verify(repositorio)
                .paginarPorStatus(eq(USUARIO), eq("lendo"), eq(inicio), any(), eq(esperado));
        verify(repositorio)
                .paginarPorStatus(eq(USUARIO), eq("desejo"), eq(inicio), any(), eq(esperado));
        verify(repositorio)
                .paginarPorStatus(eq(USUARIO), eq("lido"), eq(inicio), any(), eq(esperado));
    }

    @Test
    void deve_devolver_quatro_paginas_vazias_sem_cursor_quando_a_biblioteca_estiver_vazia() {
        BibliotecaResponse resposta = service.buscar(null);

        for (PaginaBibliotecaResponse pagina :
                List.of(
                        resposta.favoritos(),
                        resposta.lendo(),
                        resposta.desejo(),
                        resposta.lidos())) {
            assertThat(pagina.itens()).isEmpty();
            assertThat(pagina.proximoCursor()).isNull();
        }
        verify(repositorio, never()).findByIdIn(anyCollection());
    }

    @Test
    void deve_cortar_o_item_extra_e_apontar_o_cursor_para_o_ultimo_visivel() {
        List<ItemPaginado> tresItens = paginados(3);
        when(repositorio.paginarPorStatus(eq(USUARIO), eq("lido"), any(), any(), eq(3)))
                .thenReturn(tresItens);

        PaginaBibliotecaResponse pagina = service.buscarPagina("lidos", null, 2);

        assertThat(pagina.itens()).hasSize(2);
        ItemPaginado ultimoVisivel = tresItens.get(1);
        assertThat(CursorBiblioteca.decodificar(pagina.proximoCursor()))
                .isEqualTo(
                        new CursorBiblioteca(
                                ultimoVisivel.getAtividade(), ultimoVisivel.getItemId()));
    }

    @Test
    void nao_deve_devolver_cursor_quando_a_pagina_couber_inteira() {
        when(repositorio.paginarPorStatus(eq(USUARIO), eq("lido"), any(), any(), eq(3)))
                .thenReturn(paginados(2));

        PaginaBibliotecaResponse pagina = service.buscarPagina("lidos", null, 2);

        assertThat(pagina.itens()).hasSize(2);
        assertThat(pagina.proximoCursor()).isNull();
    }

    @Test
    void deve_manter_a_ordem_da_pagina_mesmo_que_o_banco_devolva_as_entidades_embaralhadas() {
        List<ItemPaginado> itens = paginados(3);
        when(repositorio.paginarFavoritos(eq(USUARIO), any(), any(), anyInt())).thenReturn(itens);

        PaginaBibliotecaResponse pagina = service.buscarPagina("favoritos", null, null);

        assertThat(pagina.itens())
                .extracting(LivroBibliotecaResponse::titulo)
                .containsExactly(
                        "Livro " + itens.get(0).getItemId(),
                        "Livro " + itens.get(1).getItemId(),
                        "Livro " + itens.get(2).getItemId());
    }

    @Test
    void deve_continuar_a_partir_do_cursor_recebido() {
        CursorBiblioteca cursor = new CursorBiblioteca(AGORA.minusSeconds(60), UUID.randomUUID());

        service.buscarPagina("desejo", cursor.codificar(), 5);

        verify(repositorio)
                .paginarPorStatus(USUARIO, "desejo", cursor.atividade(), cursor.itemId(), 6);
    }

    @Test
    void deve_usar_a_flag_de_favorito_para_a_lista_de_favoritos() {
        service.buscarPagina("favoritos", null, 5);

        verify(repositorio).paginarFavoritos(eq(USUARIO), any(), any(), eq(6));
        verify(repositorio, never()).paginarPorStatus(any(), any(), any(), any(), anyInt());
    }

    @Test
    void deve_recusar_lista_que_nao_existe_com_404_antes_de_consultar_o_banco() {
        assertThatThrownBy(() -> service.buscarPagina("recomendacoes", null, null))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("recomendacoes");
        verify(repositorio, never()).paginarPorStatus(any(), any(), any(), any(), anyInt());
        verify(repositorio, never()).paginarFavoritos(any(), any(), any(), anyInt());
    }

    @Test
    void deve_recusar_cursor_invalido() {
        assertThatThrownBy(() -> service.buscarPagina("lidos", "nao-e-um-cursor", null))
                .isInstanceOf(ParametroInvalidoException.class);
    }

    @Test
    void deve_recusar_limite_fora_da_faixa_e_aceitar_os_extremos() {
        assertThatThrownBy(() -> service.buscar(0)).isInstanceOf(ParametroInvalidoException.class);
        assertThatThrownBy(() -> service.buscar(BibliotecaService.LIMITE_MAXIMO + 1))
                .isInstanceOf(ParametroInvalidoException.class);

        service.buscarPagina("lidos", null, 1);
        service.buscarPagina("lidos", null, BibliotecaService.LIMITE_MAXIMO);

        ArgumentCaptor<Integer> limites = ArgumentCaptor.forClass(Integer.class);
        verify(repositorio, org.mockito.Mockito.times(2))
                .paginarPorStatus(eq(USUARIO), eq("lido"), any(), any(), limites.capture());
        assertThat(limites.getAllValues()).containsExactly(2, BibliotecaService.LIMITE_MAXIMO + 1);
    }

    @Test
    void deve_juntar_os_autores_na_ordem_do_vinculo_e_deixar_nulo_sem_autor() {
        ItemBiblioteca semAutor = item(livro("Anônimo"), StatusLeitura.desejo, false);
        Livro doisAutores = livro("Dupla");
        vincularAutor(doisAutores, "Primeiro");
        vincularAutor(doisAutores, "Segundo");
        ItemBiblioteca comAutores = item(doisAutores, StatusLeitura.desejo, false);
        when(repositorio.paginarPorStatus(eq(USUARIO), eq("desejo"), any(), any(), anyInt()))
                .thenReturn(List.of(paginado(comAutores.getId()), paginado(semAutor.getId())));
        when(repositorio.findByIdIn(anyCollection())).thenReturn(List.of(semAutor, comAutores));

        PaginaBibliotecaResponse pagina = service.buscarPagina("desejo", null, null);

        assertThat(pagina.itens())
                .extracting(LivroBibliotecaResponse::autor)
                .containsExactly("Primeiro, Segundo", null);
    }

    private static List<ItemPaginado> paginados(int quantidade) {
        return IntStream.range(0, quantidade)
                .mapToObj(i -> paginado(UUID.randomUUID(), AGORA.minusSeconds(i)))
                .toList();
    }

    private static ItemPaginado paginado(UUID itemId) {
        return paginado(itemId, AGORA);
    }

    private static ItemPaginado paginado(UUID itemId, Instant atividade) {
        return new ItemPaginado() {
            @Override
            public UUID getItemId() {
                return itemId;
            }

            @Override
            public Instant getAtividade() {
                return atividade;
            }
        };
    }

    // As entidades só têm construtor protegido (o Flyway e o seed são donos dos dados), então os
    // testes montam os objetos por reflexão, como o Hibernate faria.
    private static ItemBiblioteca itemComId(UUID id) {
        ItemBiblioteca item = item(livro("Livro " + id), StatusLeitura.lido, false);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private static ItemBiblioteca item(Livro livro, StatusLeitura status, boolean favorito) {
        ItemBiblioteca item = instanciar(ItemBiblioteca.class);
        ReflectionTestUtils.setField(item, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(item, "usuarioId", USUARIO);
        ReflectionTestUtils.setField(item, "livro", livro);
        ReflectionTestUtils.setField(item, "statusLeitura", status);
        ReflectionTestUtils.setField(item, "favorito", favorito);
        ReflectionTestUtils.setField(item, "paginaAtual", 0);
        ReflectionTestUtils.setField(item, "paginaMaxima", 0);
        ReflectionTestUtils.setField(item, "adicionadoEm", OffsetDateTime.now());
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
