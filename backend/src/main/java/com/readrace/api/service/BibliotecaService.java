package com.readrace.api.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.BibliotecaResponse;
import com.readrace.api.dto.response.LivroBibliotecaResponse;
import com.readrace.api.model.ItemBiblioteca;
import com.readrace.api.model.StatusLeitura;
import com.readrace.api.repository.ItemBibliotecaRepository;
import com.readrace.api.repository.ItemBibliotecaRepository.UltimaLeitura;

@Service
@Transactional(readOnly = true)
public class BibliotecaService {

    private final ItemBibliotecaRepository itemBibliotecaRepository;
    private final UsuarioAtualDeSeed usuarioAtual;

    public BibliotecaService(
            ItemBibliotecaRepository itemBibliotecaRepository, UsuarioAtualDeSeed usuarioAtual) {
        this.itemBibliotecaRepository = itemBibliotecaRepository;
        this.usuarioAtual = usuarioAtual;
    }

    /** Uma consulta só: a biblioteca inteira do usuário atual, repartida nas quatro listas. */
    public BibliotecaResponse buscar() {
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();
        List<ItemBiblioteca> itens =
                porAtividade(itemBibliotecaRepository.findByUsuarioId(usuarioId));

        return new BibliotecaResponse(
                livrosDe(itens, ItemBiblioteca::isFavorito),
                livrosDe(itens, comStatus(StatusLeitura.lendo)),
                livrosDe(itens, comStatus(StatusLeitura.desejo)),
                livrosDe(itens, comStatus(StatusLeitura.lido)));
    }

    private static Predicate<ItemBiblioteca> comStatus(StatusLeitura status) {
        return item -> item.getStatusLeitura() == status;
    }

    private static List<LivroBibliotecaResponse> livrosDe(
            List<ItemBiblioteca> itens, Predicate<ItemBiblioteca> filtro) {
        return itens.stream()
                .filter(filtro)
                .map(item -> LivroBibliotecaResponse.de(item.getLivro()))
                .toList();
    }

    /**
     * "Atividade mais recente" (#30): o último registro de leitura do item ou, para quem nunca
     * registrou, a data em que o livro entrou na biblioteca.
     */
    private List<ItemBiblioteca> porAtividade(List<ItemBiblioteca> itens) {
        if (itens.isEmpty()) {
            return itens;
        }

        Map<UUID, Instant> ultimaLeitura =
                itemBibliotecaRepository
                        .buscarUltimaLeituraPorItem(
                                itens.stream().map(ItemBiblioteca::getId).toList())
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        UltimaLeitura::getItemId, UltimaLeitura::getRegistradoEm));

        Function<ItemBiblioteca, Instant> atividade =
                item -> {
                    Instant leitura = ultimaLeitura.get(item.getId());
                    Instant adicao = item.getAdicionadoEm().toInstant();

                    return leitura != null && leitura.isAfter(adicao) ? leitura : adicao;
                };

        return itens.stream().sorted(Comparator.comparing(atividade).reversed()).toList();
    }
}
