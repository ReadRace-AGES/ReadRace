package com.readrace.api.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.SeguirResponse;
import com.readrace.api.service.SeguirService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/usuarios/{usuarioId}/seguidores/eu")
@Tag(name = "Seguidores")
public class SeguirController {
    private final SeguirService service;

    public SeguirController(SeguirService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Segue outro leitor de forma idempotente")
    public SeguirResponse seguir(@PathVariable UUID usuarioId) {
        return service.seguir(usuarioId);
    }

    @DeleteMapping
    @Operation(summary = "Deixa de seguir outro leitor de forma idempotente")
    public SeguirResponse deixarDeSeguir(@PathVariable UUID usuarioId) {
        return service.deixarDeSeguir(usuarioId);
    }
}
