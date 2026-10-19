package com.readrace.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.DesafiosSistemaResponse;
import com.readrace.api.service.DesafiosSistemaService;

@RestController
@RequestMapping("/api/desafios/sistema")
public class DesafiosSistemaController {
    private final DesafiosSistemaService service;

    public DesafiosSistemaController(DesafiosSistemaService service) {
        this.service = service;
    }

    @GetMapping
    public DesafiosSistemaResponse consultar() {
        return service.consultar();
    }
}
