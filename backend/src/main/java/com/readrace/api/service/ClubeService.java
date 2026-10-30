package com.readrace.api.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.request.CriarClubeRequest;
import com.readrace.api.dto.response.ClubeCriadoResponse;
import com.readrace.api.dto.response.ClubeResponse;
import com.readrace.api.dto.response.RankingClubeResponse;
import com.readrace.api.dto.response.SairDoClubeResponse;
import com.readrace.api.exception.ClubeInvalidoException;
import com.readrace.api.exception.ClubeNaoEncontradoException;
import com.readrace.api.exception.LivroNaoEncontradoException;
import com.readrace.api.exception.NaoEMembroException;
import com.readrace.api.exception.ParametroInvalidoException;
import com.readrace.api.exception.RecursoNaoEncontradoException;
import com.readrace.api.exception.SoMembroVeRankingException;
import com.readrace.api.exception.UsuarioNaoEncontradoException;
import com.readrace.api.model.Cargo;
import com.readrace.api.model.ClubeDoLivro;
import com.readrace.api.model.Livro;
import com.readrace.api.model.MembroClube;
import com.readrace.api.repository.ClubeDoLivroRepository;
import com.readrace.api.repository.LinhaRankingClube;
import com.readrace.api.repository.LivroRepository;
import com.readrace.api.repository.MembroClubeRepository;
import com.readrace.api.repository.QuizRepository;
import com.readrace.api.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class ClubeService {

<<<<<<< HEAD
        /** A Página do clube mostra até 7 participantes do ranking. */
=======
    /** A Página do clube mostra até 7 participantes do ranking. */
>>>>>>> c069aceb96b8b8c97717e288340e71e5b100277b
    static final int TAMANHO_DO_RANKING = 7;

    static final int TAMANHO_DA_PAGINA_DO_RANKING = 50;

    private final ClubeDoLivroRepository clubeRepository;
    private final MembroClubeRepository membroClubeRepository;
    private final QuizRepository quizRepository;
    private final UsuarioAtual usuarioAtual;

    private final LivroRepository livroRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConquistaService conquistaService;
    private final Clock clock;

    public ClubeService(
            ClubeDoLivroRepository clubeRepository,
            MembroClubeRepository membroClubeRepository,
            QuizRepository quizRepository,
            LivroRepository livroRepository,
            UsuarioRepository usuarioRepository,
            UsuarioAtual usuarioAtual,
            ConquistaService conquistaService,
            Clock clock) {
        this.clubeRepository = clubeRepository;
        this.membroClubeRepository = membroClubeRepository;
        this.quizRepository = quizRepository;
        this.livroRepository = livroRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioAtual = usuarioAtual;
        this.conquistaService = conquistaService;
        this.clock = clock;
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
        String meuCargo = meuCargo(clubeId);
        ClubeResponse.Usuario proximoLider =
                proximoLider(clubeId, meuCargo, usuarioAtual.idDoUsuarioAtual().valor());

        return new ClubeResponse(
<<<<<<< HEAD
                clube.getId(),
                clube.getNome(),
                livroAtual(clube.getLivro()),
                ranking(linhas),
                meuCargo,
                quizRepository.existsByClubeIdAndAtivoTrue(clubeId),
                proximoLider);
=======
        clube.getId(),
        clube.getNome(),
        livroAtual(clube.getLivro()),
        ranking(linhas),
        meuCargo,
        quizRepository.existsByClubeIdAndAtivoTrue(clubeId),
        proximoLider);
>>>>>>> c069aceb96b8b8c97717e288340e71e5b100277b
    }

    public RankingClubeResponse buscarRanking(UUID clubeId, String cursor) {
        ClubeDoLivro clube =
                clubeRepository
                        .findByIdAndExcluidoEmIsNull(clubeId)
                        .orElseThrow(
                                () -> new RecursoNaoEncontradoException("Clube não encontrado."));

        if (meuCargo(clubeId) == null) {
            throw new SoMembroVeRankingException();
        }

        int pagina = paginaDoCursor(cursor);
        Slice<LinhaRankingClube> linhas =
                membroClubeRepository.paginaDoRanking(
                        clubeId, PageRequest.of(pagina, TAMANHO_DA_PAGINA_DO_RANKING));

        return new RankingClubeResponse(
                new RankingClubeResponse.Clube(clube.getId(), clube.getNome()),
                ranking(linhas.getContent()),
                linhas.hasNext() ? String.valueOf(pagina + 1) : null);
    }

    private static int paginaDoCursor(String cursor) {
        if (cursor == null) {
            return 0;
        }

        if (!cursor.matches("\\d{1,6}")) {
            throw new ParametroInvalidoException("O parâmetro 'cursor' é inválido.");
        }

        return Integer.parseInt(cursor);
    }

    private String meuCargo(UUID clubeId) {
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        return membroClubeRepository
                .findByClube_IdAndUsuarioId(clubeId, usuarioId)
                .map(membro -> membro.getCargoClube().getValor())
                .orElse(null);
    }

    @Transactional
    public SairDoClubeResponse sairDoClube(UUID clubeId) {
        ClubeDoLivro clube =
                clubeRepository
                        .buscarComLock(clubeId)
                        .orElseThrow(ClubeNaoEncontradoException::new);
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        MembroClube membro =
                membroClubeRepository
                        .findByClube_IdAndUsuarioId(clubeId, usuarioId)
                        .orElseThrow(NaoEMembroException::new);

        membroClubeRepository.buscarDoClubeComLock(clubeId);

        long totalMembros = membroClubeRepository.countByClube_Id(clubeId);

        SairDoClubeResponse.NovoLider novoLider = null;
        if (totalMembros > 1
                && membro.getCargoClube() == Cargo.ADMINISTRADOR
                && membroClubeRepository.countByClube_IdAndCargoClube(clubeId, Cargo.ADMINISTRADOR)
                        == 1) {
            MembroClube sucessor =
                    membroClubeRepository
                            .candidatosALideranca(clubeId, usuarioId, PageRequest.of(0, 1))
                            .getFirst();
            sucessor.promoverAAdministrador();
            var usuario =
                    usuarioRepository
                            .findByIdAndExcluidoEmIsNull(sucessor.getUsuarioId())
                            .orElseThrow(UsuarioNaoEncontradoException::new);
            novoLider = new SairDoClubeResponse.NovoLider(usuario.getId(), usuario.getNome());
        }

        membroClubeRepository.delete(membro);

        if (totalMembros == 1) {
            clube.excluir(OffsetDateTime.now(clock));
        }
        return new SairDoClubeResponse(novoLider);
    }

    private ClubeResponse.Usuario proximoLider(UUID clubeId, String meuCargo, UUID usuarioId) {
        if (!Cargo.ADMINISTRADOR.getValor().equals(meuCargo)
                || membroClubeRepository.countByClube_IdAndCargoClube(clubeId, Cargo.ADMINISTRADOR)
                        != 1
                || membroClubeRepository.countByClube_Id(clubeId) <= 1) {
            return null;
        }
        return membroClubeRepository
                .candidatosALideranca(clubeId, usuarioId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .flatMap(
                        membro ->
                                usuarioRepository.findByIdAndExcluidoEmIsNull(
                                        membro.getUsuarioId()))
                .map(
                        usuario ->
                                new ClubeResponse.Usuario(
                                        usuario.getId(), usuario.getNome(), usuario.getAvatarUrl()))
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

        /** A consulta entrega a posição no ranking inteiro, inclusive em páginas posteriores. */
        private static List<ClubeResponse.LinhaRanking> ranking(List<LinhaRankingClube> linhas) {
                return linhas.stream().map(ClubeService::paraLinha).toList();
    }

        private static ClubeResponse.LinhaRanking paraLinha(LinhaRankingClube linha) {
        return new ClubeResponse.LinhaRanking(
                                Math.toIntExact(linha.getPosicao()),
                new ClubeResponse.Usuario(
                        linha.getUsuarioId(), linha.getNome(), linha.getAvatarUrl()),
                linha.getPontos());
    }
}
