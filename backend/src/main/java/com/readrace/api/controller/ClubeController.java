package com.readrace.api.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.request.CriarClubeRequest;
import com.readrace.api.dto.request.ResponderPerguntaRequest;
import com.readrace.api.dto.response.ClubeCriadoResponse;
import com.readrace.api.dto.response.ClubeResponse;
import com.readrace.api.dto.response.ForumClubeResponse;
import com.readrace.api.dto.response.QuizResponse;
import com.readrace.api.dto.response.RespostaQuizResponse;
import com.readrace.api.service.ClubeService;
import com.readrace.api.service.ForumClubeService;
import com.readrace.api.service.QuizService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/clubes")
@Tag(name = "Clubes", description = "A Página do clube do livro")
public class ClubeController {

    private final ClubeService clubeService;
    private final ForumClubeService forumClubeService;
    private final QuizService quizService;

    public ClubeController(
            ClubeService clubeService,
            ForumClubeService forumClubeService,
            QuizService quizService) {
        this.clubeService = clubeService;
        this.forumClubeService = forumClubeService;
        this.quizService = quizService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um clube do livro com seus primeiros membros")
    public ClubeCriadoResponse criar(@Valid @RequestBody CriarClubeRequest request) {
        return clubeService.criar(request);
    }

    @GetMapping("/{clubeId}")
    @Operation(
            summary = "Cabeçalho com o livro atual e o ranking de membros por Pontos",
            description =
                    "Só responde por clube do livro. Um id de comunidade responde 404, como"
                            + " qualquer id inexistente.")
    public ClubeResponse buscar(@PathVariable UUID clubeId) {
        return clubeService.buscar(clubeId);
    }

    @GetMapping("/{clubeId}/posts")
    @Operation(
            summary = "Posts do clube sobre a leitura atual, do mais recente para o mais antigo",
            description =
                    "Somente leitura. Só posts raiz deste clube: comentário e post de comunidade"
                            + " não entram.")
    public ForumClubeResponse buscarPosts(@PathVariable UUID clubeId) {
        return forumClubeService.buscar(clubeId);
    }

    @GetMapping("/{clubeId}/quiz")
    @Operation(
            summary = "Quiz ativo do clube e a próxima pergunta que o membro não respondeu",
            description =
                    "As alternativas não dizem qual é a correta. proximaPergunta é null quando o"
                            + " membro já respondeu todas. 404 sem quiz ativo; 403 para quem não"
                            + " é membro e para o administrador.")
    public QuizResponse buscarQuiz(@PathVariable UUID clubeId) {
        return quizService.buscar(clubeId);
    }

    @PostMapping("/{clubeId}/quiz/perguntas/{perguntaId}/resposta")
    @Operation(
            summary = "Responde uma pergunta do quiz: +50 Pontos no clube por acerto",
            description =
                    "Cada pergunta é respondida uma vez (409 na segunda). Responder a última"
                            + " conclui o quiz e paga a recompensa de XP uma única vez.")
    public RespostaQuizResponse responderPergunta(
            @PathVariable UUID clubeId,
            @PathVariable UUID perguntaId,
            @Valid @RequestBody ResponderPerguntaRequest request) {
        return quizService.responder(clubeId, perguntaId, request);
    }
}
