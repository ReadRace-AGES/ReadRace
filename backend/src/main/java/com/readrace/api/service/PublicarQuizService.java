package com.readrace.api.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.request.PublicarQuizRequest;
import com.readrace.api.dto.request.PublicarQuizRequest.PerguntaRequest;
import com.readrace.api.dto.response.QuizPublicadoResponse;
import com.readrace.api.exception.ClubeNaoEncontradoException;
import com.readrace.api.exception.QuizInvalidoException;
import com.readrace.api.exception.SoLiderCriaQuizException;
import com.readrace.api.model.AlternativaQuiz;
import com.readrace.api.model.Cargo;
import com.readrace.api.model.ClubeDoLivro;
import com.readrace.api.model.MembroClube;
import com.readrace.api.model.PerguntaQuiz;
import com.readrace.api.model.Quiz;
import com.readrace.api.repository.AlternativaQuizRepository;
import com.readrace.api.repository.ClubeDoLivroRepository;
import com.readrace.api.repository.MembroClubeRepository;
import com.readrace.api.repository.PerguntaQuizRepository;
import com.readrace.api.repository.QuizRepository;

/**
 * O líder publica o quiz do clube (#161), de uma vez: o quiz, as perguntas e as alternativas saem
 * na mesma transação. Publicar substitui o quiz ativo anterior, que deixa de aparecer para os
 * membros mas continua no banco, com as respostas e os Pontos que deu.
 */
@Service
public class PublicarQuizService {

    static final int TITULO_MAXIMO = 120;
    static final int RECOMPENSA_MINIMA = 10;
    static final int RECOMPENSA_MAXIMA = 500;
    static final List<String> LETRAS = List.of("A", "B", "C", "D");

    private final ClubeDoLivroRepository clubeRepository;
    private final MembroClubeRepository membroClubeRepository;
    private final QuizRepository quizRepository;
    private final PerguntaQuizRepository perguntaRepository;
    private final AlternativaQuizRepository alternativaRepository;
    private final UsuarioAtual usuarioAtual;

    public PublicarQuizService(
            ClubeDoLivroRepository clubeRepository,
            MembroClubeRepository membroClubeRepository,
            QuizRepository quizRepository,
            PerguntaQuizRepository perguntaRepository,
            AlternativaQuizRepository alternativaRepository,
            UsuarioAtual usuarioAtual) {
        this.clubeRepository = clubeRepository;
        this.membroClubeRepository = membroClubeRepository;
        this.quizRepository = quizRepository;
        this.perguntaRepository = perguntaRepository;
        this.alternativaRepository = alternativaRepository;
        this.usuarioAtual = usuarioAtual;
    }

    /**
     * A ordem das falhas é a do contrato: clube inexistente (404), quem não é administrador (403) e,
     * só então, o corpo (400). Nada é gravado se qualquer uma falhar.
     */
    @Transactional
    public QuizPublicadoResponse publicar(UUID clubeId, PublicarQuizRequest request) {
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        ClubeDoLivro clube =
                clubeRepository
                        .buscarAtivoComLock(clubeId)
                        .orElseThrow(ClubeNaoEncontradoException::new);
        exigirAdministrador(membroClubeRepository.findByClube_IdAndUsuarioId(clubeId, usuarioId));

        validar(request);

        quizRepository.desativarAtivoDoClube(clubeId);
        // O quiz é sempre sobre o livro atual do clube; o cliente não escolhe outro.
        Quiz quiz =
                quizRepository.saveAndFlush(
                        new Quiz(
                                clubeId,
                                clube.getLivro(),
                                usuarioId,
                                request.titulo().trim(),
                                request.recompensaXp()));

        int ordem = 1;
        for (PerguntaRequest pergunta : request.perguntas()) {
            PerguntaQuiz salva =
                    perguntaRepository.saveAndFlush(
                            new PerguntaQuiz(quiz.getId(), pergunta.enunciado().trim(), ordem++));

            for (int indice = 0; indice < LETRAS.size(); indice++) {
                alternativaRepository.save(
                        new AlternativaQuiz(
                                salva,
                                LETRAS.get(indice),
                                pergunta.alternativas().get(indice).trim(),
                                indice == pergunta.correta()));
            }
        }
        alternativaRepository.flush();

        return new QuizPublicadoResponse(
                quiz.getId(), quiz.getTitulo(), request.perguntas().size());
    }

    /** Quem não participa do clube e o membro comum recebem o mesmo 403. */
    private static void exigirAdministrador(Optional<MembroClube> membro) {
        membro.filter(m -> m.getCargoClube() == Cargo.ADMINISTRADOR)
                .orElseThrow(SoLiderCriaQuizException::new);
    }

    private static void validar(PublicarQuizRequest request) {
        if (request == null || vazio(request.titulo())) {
            throw new QuizInvalidoException("Informe o título do quiz.");
        }
        if (request.titulo().trim().length() > TITULO_MAXIMO) {
            throw new QuizInvalidoException(
                    "O título do quiz tem no máximo %d caracteres.".formatted(TITULO_MAXIMO));
        }
        Integer recompensa = request.recompensaXp();
        if (recompensa == null
                || recompensa < RECOMPENSA_MINIMA
                || recompensa > RECOMPENSA_MAXIMA) {
            throw new QuizInvalidoException(
                    "A recompensa de XP deve estar entre %d e %d."
                            .formatted(RECOMPENSA_MINIMA, RECOMPENSA_MAXIMA));
        }
        if (request.perguntas() == null || request.perguntas().isEmpty()) {
            throw new QuizInvalidoException("Adicione pelo menos uma pergunta.");
        }
        for (PerguntaRequest pergunta : request.perguntas()) {
            validarPergunta(pergunta);
        }
    }

    private static void validarPergunta(PerguntaRequest pergunta) {
        if (pergunta == null || vazio(pergunta.enunciado())) {
            throw new QuizInvalidoException("Toda pergunta precisa de um enunciado.");
        }
        List<String> alternativas = pergunta.alternativas();
        if (alternativas == null || alternativas.size() != LETRAS.size()) {
            throw new QuizInvalidoException(
                    "Toda pergunta tem exatamente %d alternativas.".formatted(LETRAS.size()));
        }
        if (alternativas.stream().anyMatch(PublicarQuizService::vazio)) {
            throw new QuizInvalidoException("Nenhuma alternativa pode ficar vazia.");
        }
        Integer correta = pergunta.correta();
        if (correta == null || correta < 0 || correta >= LETRAS.size()) {
            throw new QuizInvalidoException("Marque a alternativa correta, de 0 a 3.");
        }
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}