package com.readrace.api.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.ClubeResponse;
import com.readrace.api.exception.RecursoNaoEncontradoException;
import com.readrace.api.model.ClubeDoLivro;
import com.readrace.api.model.Livro;
import com.readrace.api.repository.ClubeDoLivroRepository;
import com.readrace.api.repository.LinhaRankingClube;
import com.readrace.api.repository.MembroClubeRepository;

@Service
@Transactional(readOnly = true)
public class ClubeService {

    /** A Página do clube mostra 7 posições; o ranking completo está fora da sprint (#35). */
    static final int TAMANHO_DO_RANKING = 7;

    private final ClubeDoLivroRepository clubeRepository;
    private final MembroClubeRepository membroClubeRepository;

    public ClubeService(
            ClubeDoLivroRepository clubeRepository, MembroClubeRepository membroClubeRepository) {
        this.clubeRepository = clubeRepository;
        this.membroClubeRepository = membroClubeRepository;
    }

    /**
     * Cabeçalho e ranking de um clube do livro.
     *
     * <p>Um id que não é de clube — inclusive um id que só existe em {@code comunidade} — não é
     * encontrado aqui e responde 404, como qualquer id inexistente.
     */
    public ClubeResponse buscar(UUID clubeId) {
        ClubeDoLivro clube =
                clubeRepository
                        .findByIdAndExcluidoEmIsNull(clubeId)
                        .orElseThrow(
                                () -> new RecursoNaoEncontradoException("Clube não encontrado."));

        List<LinhaRankingClube> linhas =
                membroClubeRepository.rankingDoClube(
                        clubeId, PageRequest.of(0, TAMANHO_DO_RANKING));

        return new ClubeResponse(
                clube.getId(), clube.getNome(), livroAtual(clube.getLivro()), ranking(linhas));
    }

    private static ClubeResponse.LivroAtual livroAtual(Livro livro) {
        String autor =
                livro.getLivroAutores().stream()
                        .map(vinculo -> vinculo.getAutor().getNome())
                        .collect(Collectors.joining(", "));

        return new ClubeResponse.LivroAtual(
                livro.getId(), livro.getTitulo(), autor.isEmpty() ? null : autor);
    }

    /**
     * A consulta já entrega ordenado; aqui só entra a numeração de 1 a {@value
     * #TAMANHO_DO_RANKING}.
     */
    private static List<ClubeResponse.LinhaRanking> ranking(List<LinhaRankingClube> linhas) {
        return IntStream.range(0, linhas.size())
                .mapToObj(indice -> paraLinha(indice + 1, linhas.get(indice)))
                .toList();
    }

    private static ClubeResponse.LinhaRanking paraLinha(int posicao, LinhaRankingClube linha) {
        return new ClubeResponse.LinhaRanking(
                posicao,
                new ClubeResponse.Usuario(
                        linha.getUsuarioId(), linha.getNome(), linha.getAvatarUrl()),
                linha.getPontos());
    }
}
