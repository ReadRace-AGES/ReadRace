package com.readrace.api.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.request.CriarDesafioRequest;
import com.readrace.api.dto.response.DesafioResponse;
import com.readrace.api.dto.response.DesafiosResponse;
import com.readrace.api.dto.response.OponentesResponse;
import com.readrace.api.service.DesafioService;

@RestController
@RequestMapping("/api/desafios")
public class DesafioController {

    private final DesafioService desafioService;

    public DesafioController(DesafioService desafioService) {
        this.desafioService = desafioService;
    }

    @GetMapping
    public ResponseEntity<DesafiosResponse> listar() {
        return ResponseEntity.ok(desafioService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DesafioResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(desafioService.buscar(id));
    }

    @GetMapping("/oponentes")
    public ResponseEntity<OponentesResponse> listarOponentes(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(desafioService.listarOponentes(q));
    }

    @PostMapping
    public ResponseEntity<DesafioResponse> criar(@Valid @RequestBody CriarDesafioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(desafioService.criar(request));
    }
}
