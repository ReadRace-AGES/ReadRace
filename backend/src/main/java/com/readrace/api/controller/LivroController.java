package com.readrace.api.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.request.CriarPostLivroRequest;
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

    @PostMapping("/{livroId}/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public LivroDetalheResponse.Post publicarPost(
            @PathVariable UUID livroId, @RequestBody CriarPostLivroRequest request) {
        return livroService.publicarPost(livroId, request.texto());
    }

    @DeleteMapping("/{livroId}/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagarPost(@PathVariable UUID livroId, @PathVariable UUID postId) {
        livroService.apagarPost(livroId, postId);
    }
}
