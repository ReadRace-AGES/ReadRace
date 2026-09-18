package com.readrace.api.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.ClubeResponse;
import com.readrace.api.dto.response.ForumClubeResponse;
import com.readrace.api.service.ClubeService;
import com.readrace.api.service.ForumClubeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/clubes")
@Tag(name = "Clubes", description = "A Página do clube do livro")
public class ClubeController {

    private final ClubeService clubeService;
    private final ForumClubeService forumClubeService;

    public ClubeController(ClubeService clubeService, ForumClubeService forumClubeService) {
        this.clubeService = clubeService;
        this.forumClubeService = forumClubeService;
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
}
