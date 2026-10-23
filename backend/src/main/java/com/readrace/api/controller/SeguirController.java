package com.readrace.api.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.ListaSeguidoresResponse;
import com.readrace.api.dto.response.SeguirResponse;
import com.readrace.api.service.SeguirService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/usuarios/{usuarioId}")
@Tag(name = "Seguidores")
public class SeguirController {
    private final SeguirService service;

    public SeguirController(SeguirService service) {
        this.service = service;
    }

    @GetMapping("/seguidores")
    @Operation(summary = "Lista seguidores do leitor, do mais recente ao mais antigo")
    public ListaSeguidoresResponse seguidores(@PathVariable UUID usuarioId) {
        return service.seguidores(usuarioId);
    }

    @GetMapping("/seguindo")
    @Operation(summary = "Lista quem o leitor segue, do mais recente ao mais antigo")
    public ListaSeguidoresResponse seguindo(@PathVariable UUID usuarioId) {
        return service.seguindo(usuarioId);
    }

    @PostMapping("/seguidores/eu")
    @Operation(summary = "Segue outro leitor de forma idempotente")
    public SeguirResponse seguir(@PathVariable UUID usuarioId) {
        return service.seguir(usuarioId);
    }

    @DeleteMapping("/seguidores/eu")
    @Operation(summary = "Deixa de seguir outro leitor de forma idempotente")
    public SeguirResponse deixarDeSeguir(@PathVariable UUID usuarioId) {
        return service.deixarDeSeguir(usuarioId);
    }
}
