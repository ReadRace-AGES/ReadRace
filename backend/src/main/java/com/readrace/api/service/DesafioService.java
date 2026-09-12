package com.readrace.api.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.request.CriarDesafioRequest;
import com.readrace.api.dto.response.DesafioResponse;
import com.readrace.api.dto.response.DesafiosResponse;
import com.readrace.api.dto.response.LivroDesafioResponse;
import com.readrace.api.dto.response.OponenteDesafioResponse;
import com.readrace.api.dto.response.OponentesResponse;
import com.readrace.api.dto.response.PlacarDesafioResponse;
import com.readrace.api.exception.DesafioNaoEncontradoException;
import com.readrace.api.exception.LivroNaoEncontradoException;
import com.readrace.api.exception.LivroObrigatorioException;
import com.readrace.api.exception.MetaInvalidaException;
import com.readrace.api.exception.OponenteNaoEAmigoException;
import com.readrace.api.exception.OponenteNaoEncontradoException;
import com.readrace.api.exception.PrazoInvalidoException;
import com.readrace.api.model.DesafioAmigo;
import com.readrace.api.model.Livro;
import com.readrace.api.model.ProgressoDesafio;
import com.readrace.api.model.StatusDesafio;
import com.readrace.api.model.TipoMetaDesafio;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.DesafioAmigoRepository;
import com.readrace.api.repository.LivroRepository;
import com.readrace.api.repository.ProgressoDesafioRepository;
import com.readrace.api.repository.SeguirRepository;
import com.readrace.api.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class DesafioService {

    private final DesafioAmigoRepository desafioRepository;
    private final ProgressoDesafioRepository progressoRepository;
    private final SeguirRepository seguirRepository;
    private final UsuarioRepository usuarioRepository;
    private final LivroRepository livroRepository;
    private final UsuarioAtualDeSeed usuarioAtual;

    public DesafioService(
            DesafioAmigoRepository desafioRepository,
            ProgressoDesafioRepository progressoRepository,
            SeguirRepository seguirRepository,
            UsuarioRepository usuarioRepository,
            LivroRepository livroRepository,
            UsuarioAtualDeSeed usuarioAtual) {
        this.desafioRepository = desafioRepository;
        this.progressoRepository = progressoRepository;
        this.seguirRepository = seguirRepository;
        this.usuarioRepository = usuarioRepository;
        this.livroRepository = livroRepository;
        this.usuarioAtual = usuarioAtual;
    }

    public DesafiosResponse listar() {
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        List<DesafioAmigo> desafios =
                desafioRepository.buscarDoUsuario(usuarioId, StatusDesafio.RECUSADO);

        if (desafios.isEmpty()) {
            return new DesafiosResponse(List.of());
        }

        List<UUID> desafioIds = new ArrayList<>();

        for (DesafioAmigo desafio : desafios) {
            desafioIds.add(desafio.getId());
        }

        List<ProgressoDesafio> progressos = progressoRepository.buscarPorDesafios(desafioIds);

        Map<UUID, List<ProgressoDesafio>> progressosPorDesafio = agruparProgressos(progressos);

        List<DesafioResponse> respostas = new ArrayList<>();

        for (DesafioAmigo desafio : desafios) {
            List<ProgressoDesafio> progressosDoDesafio =
                    progressosPorDesafio.getOrDefault(desafio.getId(), List.of());

            respostas.add(paraResponse(desafio, usuarioId, progressosDoDesafio));
        }

        return new DesafiosResponse(respostas);
    }

    public DesafioResponse buscar(UUID desafioId) {
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        DesafioAmigo desafio =
                desafioRepository
                        .buscarPorIdEUsuario(desafioId, usuarioId, StatusDesafio.RECUSADO)
                        .orElseThrow(DesafioNaoEncontradoException::new);

        List<ProgressoDesafio> progressos =
                progressoRepository.buscarPorDesafios(List.of(desafioId));

        return paraResponse(desafio, usuarioId, progressos);
    }

    public OponentesResponse listarOponentes(String termo) {
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        List<Usuario> amigos;

        if (termo == null || termo.isBlank()) {
            amigos = seguirRepository.buscarAmigos(usuarioId);
        } else {
            amigos = seguirRepository.buscarAmigosPorUsername(usuarioId, termo.trim());
        }

        List<OponenteDesafioResponse> respostas = new ArrayList<>();

        for (Usuario amigo : amigos) {
            respostas.add(OponenteDesafioResponse.de(amigo));
        }

        return new OponentesResponse(respostas);
    }

    @Transactional
    public DesafioResponse criar(CriarDesafioRequest request) {
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        validarPrazo(request.prazoDias());

        Usuario oponente =
                usuarioRepository
                        .findByIdAndExcluidoEmIsNull(request.oponenteId())
                        .orElseThrow(OponenteNaoEncontradoException::new);

        validarAmizade(usuarioId, oponente.getId());

        Usuario criador = usuarioRepository.getReferenceById(usuarioId);

        Livro livro = null;
        int metaValor;

        if (request.tipoMeta() == TipoMetaDesafio.PAGINAS) {
            validarMeta(request.meta());
            metaValor = request.meta();
        } else {
            livro = buscarLivroObrigatorio(request.livroId());
            metaValor = 1;
        }

        OffsetDateTime inicioEm = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime fimEm = inicioEm.plusDays(request.prazoDias());

        DesafioAmigo desafio =
                new DesafioAmigo(
                        criador,
                        oponente,
                        livro,
                        criarTitulo(request),
                        criarDescricao(request, livro),
                        request.tipoMeta(),
                        metaValor,
                        inicioEm,
                        fimEm);

        desafioRepository.save(desafio);

        List<ProgressoDesafio> progressos =
                List.of(
                        new ProgressoDesafio(desafio, criador),
                        new ProgressoDesafio(desafio, oponente));

        progressoRepository.saveAll(progressos);

        return paraResponse(desafio, usuarioId, progressos);
    }

    private void validarPrazo(Integer prazoDias) {
        if (prazoDias == null || prazoDias <= 0) {
            throw new PrazoInvalidoException();
        }
    }

    private void validarAmizade(UUID usuarioId, UUID oponenteId) {
        if (seguirRepository.contarAmizadeReciproca(usuarioId, oponenteId) == 0) {
            throw new OponenteNaoEAmigoException();
        }
    }

    private void validarMeta(Integer meta) {
        if (meta == null || meta < 10 || meta > 500) {
            throw new MetaInvalidaException();
        }
    }

    private Livro buscarLivroObrigatorio(UUID livroId) {
        if (livroId == null) {
            throw new LivroObrigatorioException();
        }

        return livroRepository.findById(livroId).orElseThrow(LivroNaoEncontradoException::new);
    }

    private String criarTitulo(CriarDesafioRequest request) {
        if (request.tipoMeta() == TipoMetaDesafio.PAGINAS) {
            return "Desafio de " + request.meta() + " páginas";
        }

        return "Desafio de leitura";
    }

    private String criarDescricao(CriarDesafioRequest request, Livro livro) {
        if (request.tipoMeta() == TipoMetaDesafio.PAGINAS) {
            return "Quem lê mais páginas em " + formatarPrazo(request.prazoDias());
        }

        return "Quem termina " + livro.getTitulo() + " primeiro";
    }

    private String formatarPrazo(int prazoDias) {
        return prazoDias == 1 ? "1 dia" : prazoDias + " dias";
    }

    private Map<UUID, List<ProgressoDesafio>> agruparProgressos(List<ProgressoDesafio> progressos) {
        Map<UUID, List<ProgressoDesafio>> progressosPorDesafio = new HashMap<>();

        for (ProgressoDesafio progresso : progressos) {
            UUID desafioId = progresso.getDesafio().getId();

            progressosPorDesafio.computeIfAbsent(desafioId, id -> new ArrayList<>()).add(progresso);
        }

        return progressosPorDesafio;
    }

    private DesafioResponse paraResponse(
            DesafioAmigo desafio, UUID usuarioId, List<ProgressoDesafio> progressos) {
        Usuario oponente = encontrarOponente(desafio, usuarioId);

        int progressoUsuario = encontrarProgresso(progressos, usuarioId);
        int progressoOponente = encontrarProgresso(progressos, oponente.getId());

        Integer meta =
                desafio.getTipoMeta() == TipoMetaDesafio.PAGINAS ? desafio.getMetaValor() : null;

        LivroDesafioResponse livro =
                desafio.getLivro() == null ? null : LivroDesafioResponse.de(desafio.getLivro());

        return new DesafioResponse(
                desafio.getId(),
                OponenteDesafioResponse.de(oponente),
                desafio.getDescricao(),
                desafio.getTipoMeta().getValor(),
                meta,
                livro,
                calcularPrazoDias(desafio),
                calcularDiasRestantes(desafio),
                resolverStatus(desafio.getStatus(), progressoUsuario, progressoOponente),
                new PlacarDesafioResponse(progressoUsuario, progressoOponente));
    }

    private Usuario encontrarOponente(DesafioAmigo desafio, UUID usuarioId) {
        if (desafio.getCriador().getId().equals(usuarioId)) {
            return desafio.getOponente();
        }

        return desafio.getCriador();
    }

    private int encontrarProgresso(List<ProgressoDesafio> progressos, UUID usuarioId) {
        for (ProgressoDesafio progresso : progressos) {
            if (progresso.getUsuario().getId().equals(usuarioId)) {
                return progresso.getValorAtual();
            }
        }

        throw new IllegalStateException("O desafio não possui o par de progressos esperado.");
    }

    private int calcularPrazoDias(DesafioAmigo desafio) {
        long prazo =
                ChronoUnit.DAYS.between(
                        desafio.getInicioEm().toLocalDate(), desafio.getFimEm().toLocalDate());

        return Math.toIntExact(prazo);
    }

    private int calcularDiasRestantes(DesafioAmigo desafio) {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

        long dias =
                ChronoUnit.DAYS.between(
                        agora.toLocalDate(),
                        desafio.getFimEm().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate());

        return Math.toIntExact(Math.max(dias, 0));
    }

    private String resolverStatus(
            StatusDesafio status, int progressoUsuario, int progressoOponente) {
        return switch (status) {
            case PENDENTE -> "pendente";
            case ATIVO -> "em_andamento";
            case FINALIZADO -> resolverResultado(progressoUsuario, progressoOponente);
            case RECUSADO ->
                    throw new IllegalStateException("Desafio recusado não deve ser exposto.");
        };
    }

    private String resolverResultado(int progressoUsuario, int progressoOponente) {
        if (progressoUsuario == progressoOponente) {
            throw new IllegalStateException(
                    "Não existe regra de resultado para desafios empatados.");
        }

        return progressoUsuario > progressoOponente ? "concluido_ganho" : "concluido_perdido";
    }
}
