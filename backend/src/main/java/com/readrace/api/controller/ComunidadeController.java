package com.readrace.api.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.ComunidadeResponse;
import com.readrace.api.dto.response.MembroComunidadeResponse;
import com.readrace.api.service.ComunidadeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/comunidades")
@Tag(name = "Comunidades", description = "Perfil e participação em comunidades")
public class ComunidadeController {

    private final ComunidadeService comunidadeService;

    public ComunidadeController(ComunidadeService comunidadeService) {
        this.comunidadeService = comunidadeService;
    }

    @GetMapping("/{comunidadeId}")
    @Operation(summary = "Perfil da comunidade e seus posts")
    public ComunidadeResponse buscar(@PathVariable UUID comunidadeId) {
        return comunidadeService.buscar(comunidadeId);
    }

    @PostMapping("/{comunidadeId}/membros/eu")
    @Operation(summary = "Seguir a comunidade como membro")
    public MembroComunidadeResponse seguir(@PathVariable UUID comunidadeId) {
        return comunidadeService.seguir(comunidadeId);
    }

    @DeleteMapping("/{comunidadeId}/membros/eu")
    @Operation(summary = "Deixar de seguir a comunidade")
    public MembroComunidadeResponse deixarDeSeguir(@PathVariable UUID comunidadeId) {
        return comunidadeService.deixarDeSeguir(comunidadeId);
    }
}
