package com.readrace.api.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.ClubeResponse;
import com.readrace.api.service.ClubeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/clubes")
@Tag(name = "Clubes", description = "A Página do clube do livro")
public class ClubeController {

    private final ClubeService clubeService;

    public ClubeController(ClubeService clubeService) {
        this.clubeService = clubeService;
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
}
