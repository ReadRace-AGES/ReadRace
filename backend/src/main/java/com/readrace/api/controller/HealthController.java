package com.readrace.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.HealthResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Healthcheck público da API.
 *
 * <pre>
 * GET /api/health   200 {"status":"ok"}
 * </pre>
 *
 * <p>Existe separado do {@code /actuator/health} de propósito. O Actuator é infraestrutura: checa
 * banco, tem formato próprio, e é o healthcheck do container no docker-compose. Este aqui é
 * contrato de API — o mobile só quer saber se a API responde, e o formato não pode mudar porque
 * alguém adicionou um HealthIndicator novo.
 */
@RestController
@Tag(name = "Health", description = "Verificação de disponibilidade da API")
public class HealthController {

    @GetMapping("/api/health")
    @Operation(summary = "Diz se a API está no ar")
    public HealthResponse health() {
        return HealthResponse.ok();
    }
}
