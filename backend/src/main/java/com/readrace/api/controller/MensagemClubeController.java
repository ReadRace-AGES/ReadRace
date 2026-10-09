package com.readrace.api.controller;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.request.MensagemClubeRequest;
import com.readrace.api.dto.response.MensagemClubeResponse;
import com.readrace.api.dto.response.MensagensClubeResponse;
import com.readrace.api.service.MensagemClubeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/clubes/{clubeId}/mensagens")
@Tag(name = "Chat do clube", description = "Mensagens do chat dos clubes do livro")
public class MensagemClubeController {

    private final MensagemClubeService mensagemClubeService;

    public MensagemClubeController(MensagemClubeService mensagemClubeService) {
        this.mensagemClubeService = mensagemClubeService;
    }

    @GetMapping
    @Operation(summary = "Lista as mensagens do chat do clube")
    public MensagensClubeResponse buscar(
            @PathVariable UUID clubeId,
            @RequestParam(required = false) OffsetDateTime depois) {

        return mensagemClubeService.buscar(clubeId, depois);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Envia uma mensagem para o chat do clube")
    public MensagemClubeResponse enviar(
            @PathVariable UUID clubeId,
            @RequestBody MensagemClubeRequest request) {

        return mensagemClubeService.enviar(clubeId, request);
    }
}