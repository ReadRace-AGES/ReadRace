package com.readrace.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.FeedComunidadesResponse;
import com.readrace.api.dto.response.FeedSocialResponse;
import com.readrace.api.service.FeedComunidadesService;
import com.readrace.api.service.FeedSocialService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/feed")
@Tag(name = "Feed", description = "A tela raiz do Feed, nas abas Social e Comunidades")
public class FeedController {

    private final FeedComunidadesService feedComunidadesService;
    private final FeedSocialService feedSocialService;

    public FeedController(
            FeedComunidadesService feedComunidadesService, FeedSocialService feedSocialService) {
        this.feedComunidadesService = feedComunidadesService;
        this.feedSocialService = feedSocialService;
    }

    @GetMapping("/comunidades")
    @Operation(
            summary =
                    "Clubes do livro e comunidades de que o usuário atual é membro, em duas listas")
    public FeedComunidadesResponse comunidades() {
        return feedComunidadesService.buscar();
    }

    @GetMapping("/social")
    @Operation(
            summary =
                    "Leitura dos últimos 30 dias das pessoas que o usuário atual segue, da mais"
                            + " recente para a mais antiga")
    public FeedSocialResponse social() {
        return feedSocialService.buscar();
    }
}
