package com.readrace.api.service;

import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Service;

@Service
public class HealthService {
    private final HealthEndpoint healthEndpoint;

    public HealthService(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    public boolean apiEstaSaudavel() {
        return Status.UP.equals(healthEndpoint.health().getStatus());
    }
}
