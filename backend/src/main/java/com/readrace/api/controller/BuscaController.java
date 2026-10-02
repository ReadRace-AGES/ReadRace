package com.readrace.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.BuscaResponse;
import com.readrace.api.service.BuscaService;

@RestController
@RequestMapping("/api/busca")
public class BuscaController {

    private final BuscaService buscaService;

    public BuscaController(BuscaService buscaService) {
        this.buscaService = buscaService;
    }

    @GetMapping
    public BuscaResponse<?> buscar(
            @RequestParam(name = "q", required = false) String termo,
            @RequestParam(name = "tipo", required = false) String tipo) {

        return buscaService.buscar(termo, tipo);
    }
}
