package com.readrace.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.HealthResponse;
import com.readrace.api.service.HealthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Healthcheck público da API.
 *
 * <pre>
 * GET /api/health   200 {"status":"ok"}     — atendendo normalmente
 *                   503 {"status":"down"}   — alguma dependência caiu
 * </pre>
 *
 * <p>Devolve {@code ResponseEntity} porque o status varia em tempo de execução. Endpoint de status
 * fixo não precisa disso — veja {@code UsuarioAtualController}, que devolve o DTO direto.
 *
 * <p>Existe separado do {@code /actuator/health} por causa do consumidor. Este é contrato de API,
 * de formato congelado, para o app mobile decidir se mostra a tela ou o aviso de indisponibilidade.
 * O do Actuator é diagnóstico de infraestrutura: diz <b>qual</b> dependência caiu, muda de formato
 * quando alguém adiciona um HealthIndicator, e é o healthcheck do container no docker-compose.
 */
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
