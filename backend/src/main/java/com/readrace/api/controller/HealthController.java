package com.readrace.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.HealthResponse;
import com.readrace.api.service.HealthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Health", description = "Verificação de disponibilidade da API")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/api/health")
    @Operation(summary = "Diz se a API consegue atender requisições")
    public ResponseEntity<HealthResponse> health() {
        if (healthService.apiEstaSaudavel()) {
            return ResponseEntity.ok(HealthResponse.ok());
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(HealthResponse.indisponivel());
    }
}
