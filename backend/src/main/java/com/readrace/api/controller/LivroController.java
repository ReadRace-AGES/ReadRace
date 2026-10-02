package com.readrace.api.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.LivroDetalheResponse;
import com.readrace.api.service.LivroService;

@RestController
@RequestMapping("/api/livros")
public class LivroController {

    private final LivroService livroService;

    public LivroController(LivroService livroService) {
        this.livroService = livroService;
    }

    @GetMapping("/{livroId}")
    public LivroDetalheResponse buscarDetalhe(@PathVariable UUID livroId) {
        return livroService.buscarDetalhe(livroId);
    }
}
