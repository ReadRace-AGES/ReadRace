package com.readrace.api.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.request.RegistrarProgressoRequest;
import com.readrace.api.dto.response.ProgressoLeituraResponse;
import com.readrace.api.service.RegistrarProgressoService;

@RestController
@RequestMapping("/api/livros/{livroId}/progresso")
public class ProgressoLeituraController {

    private final RegistrarProgressoService service;

    public ProgressoLeituraController(RegistrarProgressoService service) {
        this.service = service;
    }

    @PostMapping
    public ProgressoLeituraResponse registrar(
            @PathVariable UUID livroId, @RequestBody RegistrarProgressoRequest request) {
        return service.registrar(livroId, request);
    }
}
