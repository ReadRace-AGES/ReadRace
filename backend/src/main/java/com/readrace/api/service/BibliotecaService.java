package com.readrace.api.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.BibliotecaItemResponse;
import com.readrace.api.dto.response.BibliotecaResponse;
import com.readrace.api.dto.response.LivroBibliotecaResponse;
import com.readrace.api.dto.response.PaginaBibliotecaResponse;
import com.readrace.api.exception.ListaInvalidaException;
import com.readrace.api.exception.LivroComLeituraException;
import com.readrace.api.exception.LivroEmProgressoException;
import com.readrace.api.exception.LivroNaoEncontradoException;
import com.readrace.api.exception.ParametroInvalidoException;
import com.readrace.api.model.CursorBiblioteca;
import com.readrace.api.model.ItemBiblioteca;
import com.readrace.api.model.ListaBiblioteca;
import com.readrace.api.model.Livro;
import com.readrace.api.repository.ItemBibliotecaRepository;
import com.readrace.api.repository.ItemBibliotecaRepository.GeneroDoLivro;
import com.readrace.api.repository.ItemBibliotecaRepository.ItemPaginado;
import com.readrace.api.repository.LivroRepository;

@Service
@Transactional(readOnly = true)
public class BibliotecaService {

    static final int LIMITE_PADRAO = 20;
    static final int LIMITE_MAXIMO = 50;

    private final ItemBibliotecaRepository itemBibliotecaRepository;
    private final UsuarioAtual usuarioAtual;
    private final LivroRepository livroRepository;

    @Autowired
    public BibliotecaService(
            ItemBibliotecaRepository itemBibliotecaRepository,
            UsuarioAtual usuarioAtual,
            LivroRepository livroRepository) {
        this.itemBibliotecaRepository = itemBibliotecaRepository;
        this.usuarioAtual = usuarioAtual;
        this.livroRepository = livroRepository;
    }

    public BibliotecaService(
            ItemBibliotecaRepository itemBibliotecaRepository, UsuarioAtual usuarioAtual) {
        this(itemBibliotecaRepository, usuarioAtual, null);
    }

    @Transactional
    public BibliotecaItemResponse atualizarStatus(UUID livroId, String lista) {
        Livro livro =
                livroRepository.findById(livroId).orElseThrow(LivroNaoEncontradoException::new);
        if (!"lido".equals(lista) && !"desejo".equals(lista)) {
            throw new ListaInvalidaException();
        }
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();
        if ("desejo".equals(lista) || "lido".equals(lista)) {
            itemBibliotecaRepository
                    .findByUsuarioIdAndLivro_Id(usuarioId, livroId)
                    .filter(item -> item.getPaginaMaxima() > 0)
                    .ifPresent(
                            item -> {
                                throw new LivroEmProgressoException();
                            });
        }
        itemBibliotecaRepository.definirStatus(UUID.randomUUID(), usuarioId, livro.getId(), lista);
        return item(usuarioId, livroId);
    }

    @Transactional
    public BibliotecaItemResponse marcarFavorito(UUID livroId) {
        Livro livro =
                livroRepository.findById(livroId).orElseThrow(LivroNaoEncontradoException::new);
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();
        itemBibliotecaRepository.marcarFavorito(UUID.randomUUID(), usuarioId, livro.getId());
        return item(usuarioId, livroId);
    }

    @Transactional
    public BibliotecaItemResponse desmarcarFavorito(UUID livroId) {
        Livro livro =
                livroRepository.findById(livroId).orElseThrow(LivroNaoEncontradoException::new);
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();
        ItemBiblioteca item =
                itemBibliotecaRepository
                        .findByUsuarioIdAndLivro_Id(usuarioId, livro.getId())
                        .orElseThrow(LivroNaoEncontradoException::new);
        item.setFavorito(false);
        return item(usuarioId, livroId);
    }

    @Transactional
    public void remover(UUID livroId) {
        Livro livro =
                livroRepository.findById(livroId).orElseThrow(LivroNaoEncontradoException::new);
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();
        ItemBiblioteca item =
                itemBibliotecaRepository
                        .findByUsuarioIdAndLivro_Id(usuarioId, livro.getId())
                        .orElse(null);
        if (item == null) return;
        if (item.getPaginaMaxima() > 0) throw new LivroComLeituraException();
        itemBibliotecaRepository.delete(item);
    }

    private BibliotecaItemResponse item(UUID usuarioId, UUID livroId) {
        ItemBiblioteca item =
                itemBibliotecaRepository
                        .findByUsuarioIdAndLivroId(usuarioId, livroId)
                        .orElseThrow(LivroNaoEncontradoException::new);
        return new BibliotecaItemResponse(
                livroId, item.getStatusLeitura().name(), item.isFavorito());
    }

    /** A primeira página de cada lista, para a tela abrir com uma chamada só. */
    public BibliotecaResponse buscar(Integer limite) {
        int tamanho = tamanhoDaPagina(limite);
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        return new BibliotecaResponse(
                pagina(usuarioId, ListaBiblioteca.FAVORITOS, CursorBiblioteca.INICIO, tamanho),
                pagina(usuarioId, ListaBiblioteca.LENDO, CursorBiblioteca.INICIO, tamanho),
                pagina(usuarioId, ListaBiblioteca.DESEJO, CursorBiblioteca.INICIO, tamanho),
                pagina(usuarioId, ListaBiblioteca.LIDOS, CursorBiblioteca.INICIO, tamanho));
    }

    /** A página seguinte de uma lista, a partir do cursor que a página anterior devolveu. */
    public PaginaBibliotecaResponse buscarPagina(String lista, String cursor, Integer limite) {
        ListaBiblioteca listaEscolhida = ListaBiblioteca.de(lista);
        CursorBiblioteca posicao =
                cursor == null || cursor.isBlank()
                        ? CursorBiblioteca.INICIO
                        : CursorBiblioteca.decodificar(cursor);

        return pagina(
                usuarioAtual.idDoUsuarioAtual().valor(),
                listaEscolhida,
                posicao,
                tamanhoDaPagina(limite));
    }

    private static int tamanhoDaPagina(Integer limite) {
        if (limite == null) {
            return LIMITE_PADRAO;
        }

        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new ParametroInvalidoException(
                    "O parâmetro 'limite' deve estar entre 1 e %d.".formatted(LIMITE_MAXIMO));
        }

        return limite;
    }

    private PaginaBibliotecaResponse pagina(
            UUID usuarioId, ListaBiblioteca lista, CursorBiblioteca posicao, int tamanho) {
        // Pede um a mais só para saber se existe próxima página, sem contar a lista inteira.
        List<ItemPaginado> encontrados =
                lista.getStatusLeitura()
                        .map(
                                status ->
                                        itemBibliotecaRepository.paginarPorStatus(
                                                usuarioId,
                                                status.name(),
                                                posicao.atividade(),
                                                posicao.itemId(),
                                                tamanho + 1))
                        .orElseGet(
                                () ->
                                        itemBibliotecaRepository.paginarFavoritos(
                                                usuarioId,
                                                posicao.atividade(),
                                                posicao.itemId(),
                                                tamanho + 1));

        if (encontrados.isEmpty()) {
            return PaginaBibliotecaResponse.vazia();
        }

        boolean temMais = encontrados.size() > tamanho;
        List<ItemPaginado> visiveis = temMais ? encontrados.subList(0, tamanho) : encontrados;

        Map<UUID, ItemBiblioteca> porId =
                itemBibliotecaRepository
                        .findByIdIn(visiveis.stream().map(ItemPaginado::getItemId).toList())
                        .stream()
                        .collect(Collectors.toMap(ItemBiblioteca::getId, Function.identity()));

        Map<UUID, String> generoPorLivro =
                itemBibliotecaRepository
                        .buscarPrimeiroGeneroDosLivros(
                                porId.values().stream()
                                        .map(item -> item.getLivro().getId())
                                        .toList())
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        GeneroDoLivro::getLivroId, GeneroDoLivro::getNome));

        List<LivroBibliotecaResponse> livros =
                visiveis.stream()
                        .map(item -> porId.get(item.getItemId()).getLivro())
                        .map(
                                livro ->
                                        LivroBibliotecaResponse.de(
                                                livro, generoPorLivro.get(livro.getId())))
                        .toList();

        ItemPaginado ultimo = visiveis.getLast();
        String proximoCursor =
                temMais
                        ? new CursorBiblioteca(ultimo.getAtividade(), ultimo.getItemId())
                                .codificar()
                        : null;

        return new PaginaBibliotecaResponse(livros, proximoCursor);
    }
}
