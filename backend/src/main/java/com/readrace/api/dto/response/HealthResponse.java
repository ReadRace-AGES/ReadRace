package com.readrace.api.dto.response;

/**
 * Resposta do healthcheck público.
 *
 * <p>Record em vez de Map: o contrato fica declarado no tipo, aparece no Swagger e não corre o
 * risco de alguém trocar a chave sem perceber.
 *
 * @param status {@code "ok"} quando a API atende, {@code "down"} quando alguma dependência caiu
 */
public record HealthResponse(String status) {

    public static HealthResponse ok() {
        return new HealthResponse("ok");
    }

    public static HealthResponse indisponivel() {
        return new HealthResponse("down");
    }
}
