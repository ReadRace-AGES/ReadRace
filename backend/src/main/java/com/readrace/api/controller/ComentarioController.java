package com.readrace.api.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.request.CriarComentarioRequest;
import com.readrace.api.dto.response.ComentariosResponse;
import com.readrace.api.service.ComentarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/posts/{postId}/comentarios")
@Tag(name = "Posts", description = "Curtidas e comentários de posts")
public class ComentarioController {

    private final ComentarioService comentarioService;

    public ComentarioController(ComentarioService comentarioService) {
        this.comentarioService = comentarioService;
    }

    @GetMapping
    @Operation(
            summary = "Thread de comentários de um post de comunidade, em árvore",
            description =
                    "Cada nível vem do mais antigo para o mais novo, com curtidas e respostas. Com o"
                            + " id de um comentário, devolve as respostas dele. Quem não segue a"
                            + " comunidade também lê; podeComentar diz se pode escrever. Post de"
                            + " clube ou de livro responde 404.")
    public ComentariosResponse listar(@PathVariable UUID postId) {
        return comentarioService.listar(postId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Comenta um post de comunidade ou responde a um comentário",
            description =
                    "Com o id de um comentário, cria uma resposta a ele. De 1 a 1.000 caracteres;"
                            + " só espaços não conta. 403 para quem não segue a comunidade.")
    public ComentariosResponse.Comentario comentar(
            @PathVariable UUID postId, @RequestBody CriarComentarioRequest request) {
        return comentarioService.comentar(postId, request.texto());
    }
}
