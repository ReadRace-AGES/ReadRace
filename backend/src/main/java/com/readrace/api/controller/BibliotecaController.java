package com.readrace.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.request.AdicionarLivroRequest;
import com.readrace.api.dto.response.AdicionarLivroResponse;
import com.readrace.api.dto.response.BibliotecaResponse;
import com.readrace.api.dto.response.PaginaBibliotecaResponse;
import com.readrace.api.service.AdicionarLivroService;
import com.readrace.api.service.BibliotecaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/biblioteca")
@Tag(name = "Biblioteca", description = "A biblioteca do usuário atual, como a aba Meus Livros vê")
public class BibliotecaController {

    private final BibliotecaService bibliotecaService;
    private final AdicionarLivroService adicionarLivroService;

    public BibliotecaController(
            BibliotecaService bibliotecaService, AdicionarLivroService adicionarLivroService) {
        this.bibliotecaService = bibliotecaService;
        this.adicionarLivroService = adicionarLivroService;
    }

    @GetMapping
    @Operation(
            summary =
                    "Primeira página de favoritos, em leitura, desejados e lidos do usuário atual")
    public BibliotecaResponse buscar(
            @RequestParam(name = "limite", required = false) Integer limite) {
        return bibliotecaService.buscar(limite);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Adiciona um livro do catálogo à biblioteca do usuário atual",
            description =
                    "Busca o volume pelo catálogo (não confia no que o app manda). Cria o livro se"
                            + " ainda não existir (pelo ISBN) e cria ou atualiza o item da"
                            + " biblioteca, sem duplicar.")
    public AdicionarLivroResponse adicionar(@RequestBody AdicionarLivroRequest request) {
        return adicionarLivroService.adicionar(request);
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
