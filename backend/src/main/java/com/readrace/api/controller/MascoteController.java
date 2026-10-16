package com.readrace.api.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.request.EscolherMascoteRequest;
import com.readrace.api.dto.response.MascoteResponse;
import com.readrace.api.dto.response.MascotesResponse;
import com.readrace.api.service.MascoteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Mascotes", description = "Mascotes desbloqueados pelo nível do usuário atual")
public class MascoteController {
    private final MascoteService service;

    public MascoteController(MascoteService service) {
        this.service = service;
    }

    @GetMapping("/api/me/mascotes")
    @Operation(summary = "Lista os mascotes na ordem da tela, com desbloqueio e o escolhido")
    public MascotesResponse listar() {
        return service.listarDoUsuarioAtual();
    }

    @PutMapping("/api/me/mascote")
    @Operation(summary = "Escolhe o mascote do usuário atual; 409 se ainda estiver bloqueado")
    public MascoteResponse escolher(@Valid @RequestBody EscolherMascoteRequest request) {
        return service.escolherDoUsuarioAtual(request.mascoteId());
    }
}
