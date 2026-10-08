package com.readrace.api.adapter.cognito;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Perfil da pessoa no Cognito, lido pelo GetUser. email_verified é guardado como Object porque
 * provedores diferentes o mandam como boolean ou string.
 */
public record CognitoUserInfo(
        String sub,
        String email,
        @JsonProperty("email_verified") Object emailVerified,
        String name,
        String picture) {

    public boolean emailVerificado() {
        return "true".equalsIgnoreCase(String.valueOf(emailVerified));
    }
}
