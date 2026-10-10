
package com.readrace.api.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.ClubeResponse;
import com.readrace.api.exception.NaoEMembroException;
import com.readrace.api.exception.RecursoNaoEncontradoException;
import com.readrace.api.exception.UnicoLiderException;
import com.readrace.api.model.Cargo;
import com.readrace.api.model.ClubeDoLivro;
import com.readrace.api.model.Livro;
import com.readrace.api.model.MembroClube;
import com.readrace.api.repository.ClubeDoLivroRepository;
import com.readrace.api.repository.LinhaRankingClube;
import com.readrace.api.repository.MembroClubeRepository;

@Service
@Transactional(readOnly = true)
public class ClubeService {

    static final int TAMANHO_DO_RANKING = 7;

    private final ClubeDoLivroRepository clubeRepository;
    private final MembroClubeRepository membroClubeRepository;
    private final UsuarioAtual usuarioAtual;

    public ClubeService(
            ClubeDoLivroRepository clubeRepository,
            MembroClubeRepository membroClubeRepository,
            UsuarioAtual usuarioAtual) {
        this.clubeRepository = clubeRepository;
        this.membroClubeRepository = membroClubeRepository;
        this.usuarioAtual = usuarioAtual;
    }

    public ClubeResponse buscar(UUID clubeId) {
        ClubeDoLivro clube =
                clubeRepository
                        .findByIdAndExcluidoEmIsNull(clubeId)
                        .orElseThrow(
                                () -> new RecursoNaoEncontradoException(
                                        "Clube não encontrado."));

        List<LinhaRankingClube> linhas =
                membroClubeRepository.rankingDoClube(
                        clubeId, PageRequest.of(0, TAMANHO_DO_RANKING));

        return new ClubeResponse(
                clube.getId(),
                clube.getNome(),
                livroAtual(clube.getLivro()),
                ranking(linhas),
                meuCargo(clubeId));
    }

    private String meuCargo(UUID clubeId) {
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        return membroClubeRepository
                .findByClube_IdAndUsuarioId(clubeId, usuarioId)
                .map(membro -> membro.getCargoClube().getValor())
                .orElse(null);
    }

    // Task #152 - Sair de um clube do livro

    @Transactional
    public void sairDoClube(UUID clubeId) {

        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        MembroClube membro =
                membroClubeRepository
                        .findByClube_IdAndUsuarioId(clubeId, usuarioId)
                        .orElseThrow(NaoEMembroException::new);

        if (membro.getCargoClube() == Cargo.ADMINISTRADOR) {

            long totalMembros =
                    membroClubeRepository.countByClube_Id(clubeId);

            long totalAdministradores =
                    membroClubeRepository.countByClube_IdAndCargoClube(
                            clubeId, Cargo.ADMINISTRADOR);

            if (totalAdministradores == 1 && totalMembros > 1) {
                throw new UnicoLiderException();
            }
        }

        membroClubeRepository.delete(membro);
    }

    private static ClubeResponse.LivroAtual livroAtual(Livro livro) {
        String autor =
                livro.getLivroAutores().stream()
                        .map(vinculo -> vinculo.getAutor().getNome())
                        .collect(Collectors.joining(", "));

        return new ClubeResponse.LivroAtual(
                livro.getId(),
                livro.getTitulo(),
                autor.isEmpty() ? null : autor);
    }

    private static List<ClubeResponse.LinhaRanking> ranking(
            List<LinhaRankingClube> linhas) {

        return IntStream.range(0, linhas.size())
                .mapToObj(indice -> paraLinha(indice + 1, linhas.get(indice)))
                .toList();
    }

    private static ClubeResponse.LinhaRanking paraLinha(
            int posicao,
            LinhaRankingClube linha) {

        return new ClubeResponse.LinhaRanking(
                posicao,
                new ClubeResponse.Usuario(
                        linha.getUsuarioId(),
                        linha.getNome(),
                        linha.getAvatarUrl()),
                linha.getPontos());
    }
}
