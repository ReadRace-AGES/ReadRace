package com.readrace.api.dto.response;

public record HealthResponse(String status) {
    public static HealthResponse ok() {
        return new HealthResponse("ok");
    }

    public static HealthResponse indisponivel() {
        return new HealthResponse("down");
    }
}
