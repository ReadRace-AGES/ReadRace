package com.readrace.api.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.request.ExemploRequest;
import com.readrace.api.dto.response.ExemploResponse;
import com.readrace.api.service.ExemploService;

/**
 * Camada HTTP: recebe, valida o formato, delega e devolve.
 *
 * <p>Sem if de regra de negócio, sem repository e sem try/catch aqui.
 *
 * <pre>
 * GET    /api/exemplos        200
 * GET    /api/exemplos/{id}   200 | 404
 * POST   /api/exemplos        201 | 400
 * PUT    /api/exemplos/{id}   200 | 400 | 404
 * DELETE /api/exemplos/{id}   204 | 404
 * </pre>
 */
@RestController
@RequestMapping("/api/exemplos")
public class ExemploController {

    private final ExemploService exemploService;

    public ExemploController(ExemploService exemploService) {
        this.exemploService = exemploService;
    }

    // Sem rota própria: a URL é a da classe, /api/exemplos.
    @GetMapping
    public List<ExemploResponse> listar() {
        return exemploService.listar();
    }

    @GetMapping("/{id}")
    public ExemploResponse buscarPorId(@PathVariable Long id) {
        return exemploService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<ExemploResponse> criar(@RequestBody @Valid ExemploRequest request) {
        ExemploResponse exemplo = exemploService.criar(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(exemplo);
    }

    @PutMapping("/{id}")
    public ExemploResponse atualizar(
            @PathVariable Long id, @RequestBody @Valid ExemploRequest request) {
        return exemploService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        exemploService.excluir(id);
    }
}
