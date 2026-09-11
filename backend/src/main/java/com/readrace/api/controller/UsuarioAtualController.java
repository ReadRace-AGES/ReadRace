package com.readrace.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.UsuarioAtualResponse;
import com.readrace.api.service.UsuarioAtualService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Usuário atual", description = "Dados de quem está usando o app")
public class UsuarioAtualController {
    private final UsuarioAtualService usuarioAtualService;

    public UsuarioAtualController(UsuarioAtualService usuarioAtualService) {
        this.usuarioAtualService = usuarioAtualService;
    }

    @GetMapping("/api/me")
    @Operation(summary = "Devolve o usuário atual, resolvido pelo backend")
    public UsuarioAtualResponse eu() {
        return usuarioAtualService.buscar();
    }
}
