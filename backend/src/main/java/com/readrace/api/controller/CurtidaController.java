package com.readrace.api.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.CurtidaResponse;
import com.readrace.api.service.CurtidaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/posts/{postId}/curtida")
@Tag(name = "Posts", description = "Curtir e descurtir posts")
public class CurtidaController {

    private final CurtidaService curtidaService;

    public CurtidaController(CurtidaService curtidaService) {
        this.curtidaService = curtidaService;
    }

    @PostMapping
    @Operation(
            summary = "Curte um post",
            description = "Idempotente: curtir de novo devolve 200 sem criar uma segunda curtida.")
    public CurtidaResponse curtir(@PathVariable UUID postId) {
        return curtidaService.curtir(postId);
    }

    @DeleteMapping
    @Operation(
            summary = "Descurte um post",
            description = "Idempotente: descurtir um post nunca curtido devolve 200 sem erro.")
    public CurtidaResponse descurtir(@PathVariable UUID postId) {
        return curtidaService.descurtir(postId);
    }
}
