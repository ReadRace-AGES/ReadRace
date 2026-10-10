package com.readrace.api.service;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.request.CriarClubeRequest;
import com.readrace.api.dto.response.ClubeCriadoResponse;
import com.readrace.api.dto.response.ClubeResponse;
import com.readrace.api.exception.ClubeInvalidoException;
import com.readrace.api.exception.LivroNaoEncontradoException;
import com.readrace.api.exception.RecursoNaoEncontradoException;
import com.readrace.api.exception.UsuarioNaoEncontradoException;
import com.readrace.api.model.Cargo;
import com.readrace.api.model.ClubeDoLivro;
import com.readrace.api.model.Livro;
import com.readrace.api.model.MembroClube;
import com.readrace.api.repository.ClubeDoLivroRepository;
import com.readrace.api.repository.LinhaRankingClube;
import com.readrace.api.repository.LivroRepository;
import com.readrace.api.repository.MembroClubeRepository;
import com.readrace.api.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class ClubeService {

    /** A Página do clube mostra 7 posições; o ranking completo está fora da sprint (#35). */
    static final int TAMANHO_DO_RANKING = 7;

    private final ClubeDoLivroRepository clubeRepository;
    private final MembroClubeRepository membroClubeRepository;
    private final UsuarioAtual usuarioAtual;

    private final LivroRepository livroRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConquistaService conquistaService;

    public ClubeService(
            ClubeDoLivroRepository clubeRepository,
            MembroClubeRepository membroClubeRepository,
            LivroRepository livroRepository,
            UsuarioRepository usuarioRepository,
            UsuarioAtual usuarioAtual,
            ConquistaService conquistaService) {
        this.clubeRepository = clubeRepository;
        this.membroClubeRepository = membroClubeRepository;
        this.livroRepository = livroRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioAtual = usuarioAtual;
        this.conquistaService = conquistaService;
    }

    @Transactional
    public ClubeCriadoResponse criar(CriarClubeRequest request) {
        if (request.nome() == null || request.nome().isBlank() || request.nome().length() > 120) {
            throw new ClubeInvalidoException();
        }

        Livro livro =
                livroRepository
                        .findById(request.livroId())
                        .orElseThrow(LivroNaoEncontradoException::new);
        UUID criadorId = usuarioAtual.idDoUsuarioAtual().valor();
        Set<UUID> participantes = new TreeSet<>();
        participantes.add(criadorId);
        if (request.membros() != null) {
            participantes.addAll(request.membros());
        }

        // Mesma ordem de locks das conquistas. Adquirir antes dos INSERTs evita promover
        // locks de FK de vários participantes em ordens diferentes entre requisições.
        for (UUID participante : participantes) {
            usuarioRepository
                    .buscarAtivoComLock(participante)
                    .orElseThrow(UsuarioNaoEncontradoException::new);
        }

        ClubeDoLivro clube =
                clubeRepository.save(new ClubeDoLivro(request.nome(), request.descricao(), livro));
        membroClubeRepository.saveAll(
                participantes.stream()
                        .map(
                                id ->
                                        new MembroClube(
                                                clube,
                                                id,
                                                id.equals(criadorId)
                                                        ? Cargo.ADMINISTRADOR
                                                        : Cargo.MEMBRO))
                        .toList());

        // avaliar faz flush antes das medições, incluindo os vínculos desta transação.
        participantes.forEach(conquistaService::avaliar);
        return ClubeCriadoResponse.de(clube);
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
