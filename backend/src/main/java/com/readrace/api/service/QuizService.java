package com.readrace.api.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.request.ResponderPerguntaRequest;
import com.readrace.api.dto.response.QuizResponse;
import com.readrace.api.dto.response.RespostaQuizResponse;
import com.readrace.api.exception.AlternativaInvalidaException;
import com.readrace.api.exception.PerguntaJaRespondidaException;
import com.readrace.api.exception.QuizNaoEncontradoException;
import com.readrace.api.exception.RecursoNaoEncontradoException;
import com.readrace.api.exception.SoMembroRespondeException;
import com.readrace.api.exception.UsuarioNaoEncontradoException;
import com.readrace.api.model.AlternativaQuiz;
import com.readrace.api.model.Cargo;
import com.readrace.api.model.ConclusaoQuiz;
import com.readrace.api.model.MembroClube;
import com.readrace.api.model.PerguntaQuiz;
import com.readrace.api.model.Quiz;
import com.readrace.api.model.RespostaQuiz;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.AlternativaQuizRepository;
import com.readrace.api.repository.ClubeDoLivroRepository;
import com.readrace.api.repository.ConclusaoQuizRepository;
import com.readrace.api.repository.MembroClubeRepository;
import com.readrace.api.repository.PerguntaQuizRepository;
import com.readrace.api.repository.QuizRepository;
import com.readrace.api.repository.RespostaQuizRepository;
import com.readrace.api.repository.UsuarioRepository;

/**
 * O membro responde o quiz ativo do clube (#162).
 *
 * <p>Cada acerto soma {@value #PONTOS_POR_ACERTO} {@code Pontos} no ranking do clube. Responder a
 * última pergunta conclui o quiz e paga a recompensa de {@code XP} do líder, uma única vez. As duas
 * moedas nunca se convertem. O administrador nunca responde o próprio quiz.
 */
@Service
@Transactional(readOnly = true)
public class QuizService {

    static final int PONTOS_POR_ACERTO = 50;

    private final ClubeDoLivroRepository clubeRepository;
    private final MembroClubeRepository membroClubeRepository;
    private final QuizRepository quizRepository;
    private final PerguntaQuizRepository perguntaRepository;
    private final AlternativaQuizRepository alternativaRepository;
    private final RespostaQuizRepository respostaRepository;
    private final ConclusaoQuizRepository conclusaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioAtual usuarioAtual;

    public QuizService(
            ClubeDoLivroRepository clubeRepository,
            MembroClubeRepository membroClubeRepository,
            QuizRepository quizRepository,
            PerguntaQuizRepository perguntaRepository,
            AlternativaQuizRepository alternativaRepository,
            RespostaQuizRepository respostaRepository,
            ConclusaoQuizRepository conclusaoRepository,
            UsuarioRepository usuarioRepository,
            UsuarioAtual usuarioAtual) {
        this.clubeRepository = clubeRepository;
        this.membroClubeRepository = membroClubeRepository;
        this.quizRepository = quizRepository;
        this.perguntaRepository = perguntaRepository;
        this.alternativaRepository = alternativaRepository;
        this.respostaRepository = respostaRepository;
        this.conclusaoRepository = conclusaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioAtual = usuarioAtual;
    }

    public QuizResponse buscar(UUID clubeId) {
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();
        exigirClube(clubeId);
        exigirMembroComum(membroClubeRepository.findByClube_IdAndUsuarioId(clubeId, usuarioId));
        Quiz quiz = quizAtivo(clubeId);

        PerguntaQuiz proxima =
                perguntaRepository
                        .naoRespondidas(quiz.getId(), usuarioId, PageRequest.of(0, 1))
                        .stream()
                        .findFirst()
                        .orElse(null);

        return QuizResponse.de(quiz, (int) perguntaRepository.countByQuizId(quiz.getId()), proxima);
    }

    /**
     * Grava a resposta, os pontos, a conclusão e o XP na mesma transação. O lock na linha do membro
     * serializa envios repetidos: o segundo enxerga a resposta do primeiro e recebe 409.
     */
    @Transactional
    public RespostaQuizResponse responder(
            UUID clubeId, UUID perguntaId, ResponderPerguntaRequest request) {
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();
        exigirClube(clubeId);
        MembroClube membro =
                exigirMembroComum(membroClubeRepository.buscarComLock(clubeId, usuarioId));
        Quiz quiz = quizAtivo(clubeId);

        PerguntaQuiz pergunta =
                perguntaRepository
                        .findByIdAndQuizId(perguntaId, quiz.getId())
                        .orElseThrow(
                                () ->
                                        new RecursoNaoEncontradoException(
                                                "Pergunta não encontrada."));

        if (respostaRepository.existsByPerguntaIdAndUsuarioId(pergunta.getId(), usuarioId)) {
            throw new PerguntaJaRespondidaException();
        }

        AlternativaQuiz alternativa =
                alternativaRepository
                        .findByIdAndPergunta_Id(request.alternativaId(), pergunta.getId())
                        .orElseThrow(AlternativaInvalidaException::new);

        respostaRepository.save(new RespostaQuiz(usuarioId, alternativa));

        boolean acertou = alternativa.getCorreta();
        int pontosGanhos = acertou ? PONTOS_POR_ACERTO : 0;
        membro.somarPontos(pontosGanhos);

        // O save acima ainda não foi para o banco; o flush garante que a contagem enxergue esta
        // resposta.
        respostaRepository.flush();
        int total = (int) perguntaRepository.countByQuizId(quiz.getId());
        boolean concluiu = respostaRepository.contarRespondidas(quiz.getId(), usuarioId) == total;

        RespostaQuizResponse.Resumo resumo = concluiu ? concluir(quiz, usuarioId, total) : null;

        return new RespostaQuizResponse(acertou, pontosGanhos, concluiu, resumo);
    }

    private RespostaQuizResponse.Resumo concluir(Quiz quiz, UUID usuarioId, int total) {
        conclusaoRepository.save(new ConclusaoQuiz(quiz, usuarioId));

        // Mesmo lock do registro de leitura: duas somas de XP no mesmo usuário não se sobrescrevem.
        Usuario usuario =
                usuarioRepository
                        .buscarAtivoComLock(usuarioId)
                        .orElseThrow(UsuarioNaoEncontradoException::new);
        usuario.receberXp(quiz.getRecompensaXp());

        int acertos = (int) respostaRepository.contarAcertos(quiz.getId(), usuarioId);
        return new RespostaQuizResponse.Resumo(
                acertos, total, acertos * PONTOS_POR_ACERTO, quiz.getRecompensaXp());
    }

    private void exigirClube(UUID clubeId) {
        if (clubeRepository.findByIdAndExcluidoEmIsNull(clubeId).isEmpty()) {
            throw new RecursoNaoEncontradoException("Clube não encontrado.");
        }
    }

    /** Quem não participa do clube e o administrador recebem o mesmo 403. */
    private static MembroClube exigirMembroComum(Optional<MembroClube> membro) {
        return membro.filter(m -> m.getCargoClube() == Cargo.MEMBRO)
                .orElseThrow(SoMembroRespondeException::new);
    }

    private Quiz quizAtivo(UUID clubeId) {
        return quizRepository
                .findByClubeIdAndAtivoTrue(clubeId)
                .orElseThrow(QuizNaoEncontradoException::new);
    }
}
