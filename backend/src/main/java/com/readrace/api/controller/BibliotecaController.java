package com.readrace.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.BibliotecaResponse;
import com.readrace.api.dto.response.PaginaBibliotecaResponse;
import com.readrace.api.service.BibliotecaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/biblioteca")
@Tag(name = "Biblioteca", description = "A biblioteca do usuário atual, como a aba Meus Livros vê")
public class BibliotecaController {

    private final BibliotecaService bibliotecaService;

    public BibliotecaController(BibliotecaService bibliotecaService) {
        this.bibliotecaService = bibliotecaService;
    }

    @GetMapping
    @Operation(
            summary =
                    "Primeira página de favoritos, em leitura, desejados e lidos do usuário atual")
    public BibliotecaResponse buscar(
            @RequestParam(name = "limite", required = false) Integer limite) {
        return bibliotecaService.buscar(limite);
    }

    @GetMapping("/{lista}")
    @Operation(summary = "Página seguinte de uma lista da biblioteca, a partir do cursor")
    public PaginaBibliotecaResponse buscarPagina(
            @PathVariable String lista,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limite", required = false) Integer limite) {
        return bibliotecaService.buscarPagina(lista, cursor, limite);
    }
}
