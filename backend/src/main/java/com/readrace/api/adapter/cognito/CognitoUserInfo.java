package com.readrace.api.adapter.cognito;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resposta do endpoint /oauth2/userInfo do Cognito. email_verified vem como boolean ou string
 * dependendo do provedor, por isso Object.
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
