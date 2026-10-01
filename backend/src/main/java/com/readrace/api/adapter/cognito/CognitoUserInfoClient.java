package com.readrace.api.adapter.cognito;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Busca nome, e-mail e foto no Cognito. O access token só traz o "sub"; os dados de perfil vêm
 * daqui, e não do app, porque o que o cliente envia não é confiável.
 */
@Component
@ConditionalOnProperty(name = "readrace.auth.modo", havingValue = "cognito")
public class CognitoUserInfoClient {
    private final RestClient restClient;

    public CognitoUserInfoClient(@Value("${readrace.cognito.domain}") String domain) {
        this.restClient = RestClient.builder().baseUrl(domain).build();
    }

    public CognitoUserInfo buscar(String accessToken) {
        return restClient
                .get()
                .uri("/oauth2/userInfo")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(CognitoUserInfo.class);
    }
}
