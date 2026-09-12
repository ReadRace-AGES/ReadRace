package com.readrace.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.BibliotecaResponse;
import com.readrace.api.service.BibliotecaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Biblioteca", description = "A biblioteca do usuário atual, como a aba Meus Livros vê")
public class BibliotecaController {

    private final BibliotecaService bibliotecaService;

    public BibliotecaController(BibliotecaService bibliotecaService) {
        this.bibliotecaService = bibliotecaService;
    }

    @GetMapping("/api/biblioteca")
    @Operation(summary = "Favoritos, em leitura, desejados e lidos do usuário atual")
    public BibliotecaResponse buscar() {
        return bibliotecaService.buscar();
    }
}
