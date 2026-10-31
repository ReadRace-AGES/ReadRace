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
            summary = "Lista comentários e respostas de um post",
            description =
                    "Retorna os comentários de posts de comunidade, clube ou livro "
                            + "em formato de árvore, incluindo curtidas e respostas. "
                            + "Os comentários são ordenados do mais antigo para o mais novo. "
                            + "O campo podeComentar indica se o usuário atual tem "
                            + "permissão para escrever. A leitura é pública.")
    public ComentariosResponse listar(@PathVariable UUID postId) {
        return comentarioService.listar(postId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cria um comentário ou responde a outro comentário",
            description =
                    "Permite comentar posts de comunidade, clube ou livro, "
                            + "assim como responder a comentários existentes. "
                            + "O texto deve conter de 1 a 1.000 caracteres e não "
                            + "pode ser composto apenas por espaços. "
                            + "Em comunidades, somente seguidores podem comentar. "
                            + "Em clubes, somente membros podem comentar. "
                            + "Em posts de livros, qualquer usuário autenticado pode comentar. "
                            + "Retorna 403 quando o usuário não possui permissão "
                            + "e 404 quando o post não existe ou foi excluído.")
    public ComentariosResponse.Comentario comentar(
            @PathVariable UUID postId,
            @RequestBody CriarComentarioRequest request) {

        return comentarioService.comentar(postId, request.texto());
    }
}