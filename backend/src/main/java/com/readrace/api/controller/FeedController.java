package com.readrace.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.FeedComunidadesResponse;
import com.readrace.api.service.FeedComunidadesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/feed")
@Tag(name = "Feed", description = "A tela raiz do Feed, na aba Comunidades")
public class FeedController {

    private final FeedComunidadesService feedComunidadesService;

    public FeedController(FeedComunidadesService feedComunidadesService) {
        this.feedComunidadesService = feedComunidadesService;
    }

    @GetMapping("/comunidades")
    @Operation(
            summary =
                    "Clubes do livro e comunidades de que o usuário atual é membro, em duas listas")
    public FeedComunidadesResponse comunidades() {
        return feedComunidadesService.buscar();
    }
}
