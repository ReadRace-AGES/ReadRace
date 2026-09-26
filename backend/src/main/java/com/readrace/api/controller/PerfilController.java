package com.readrace.api.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.PerfilResponse;
import com.readrace.api.service.PerfilService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Perfil público")
public class PerfilController {
    private final PerfilService service;

    public PerfilController(PerfilService service) {
        this.service = service;
    }

    @GetMapping("/api/usuarios/{usuarioId}/perfil")
    @Operation(summary = "Consulta o perfil público; xpAtual é o XP total armazenado")
    public PerfilResponse buscar(@PathVariable UUID usuarioId) {
        return service.buscar(usuarioId);
    }

    @GetMapping("/api/me/perfil")
    @Operation(summary = "Consulta o perfil do usuário atual, resolvido pelo backend")
    public PerfilResponse meuPerfil() {
        return service.buscarDoUsuarioAtual();
    }
}
