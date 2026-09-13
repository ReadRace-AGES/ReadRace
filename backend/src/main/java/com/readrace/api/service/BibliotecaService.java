package com.readrace.api.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.BibliotecaResponse;
import com.readrace.api.dto.response.LivroBibliotecaResponse;
import com.readrace.api.dto.response.PaginaBibliotecaResponse;
import com.readrace.api.exception.ParametroInvalidoException;
import com.readrace.api.model.CursorBiblioteca;
import com.readrace.api.model.ItemBiblioteca;
import com.readrace.api.model.ListaBiblioteca;
import com.readrace.api.repository.ItemBibliotecaRepository;
import com.readrace.api.repository.ItemBibliotecaRepository.ItemPaginado;

@Service
@Transactional(readOnly = true)
public class BibliotecaService {

    static final int LIMITE_PADRAO = 20;
    static final int LIMITE_MAXIMO = 50;

    private final ItemBibliotecaRepository itemBibliotecaRepository;
    private final UsuarioAtualDeSeed usuarioAtual;

    public BibliotecaService(
            ItemBibliotecaRepository itemBibliotecaRepository, UsuarioAtualDeSeed usuarioAtual) {
        this.itemBibliotecaRepository = itemBibliotecaRepository;
        this.usuarioAtual = usuarioAtual;
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

        List<LivroBibliotecaResponse> livros =
                visiveis.stream()
                        .map(
                                item ->
                                        LivroBibliotecaResponse.de(
                                                porId.get(item.getItemId()).getLivro()))
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
