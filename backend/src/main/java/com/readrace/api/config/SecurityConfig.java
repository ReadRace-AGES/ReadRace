package com.readrace.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.Assert;

import com.readrace.api.dto.response.ErroResponse;
import com.readrace.api.exception.CodigoErro;

import tools.jackson.databind.ObjectMapper;

/**
 * Dois modos, escolhidos por {@code readrace.auth.modo}:
 *
 * <ul>
 *   <li>{@code seed}: tudo liberado, o usuário é o seed. Dev e testes.
 *   <li>{@code cognito}: exige Bearer JWT do Cognito. Produção.
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLICOS = {
        "/api/health",
        "/actuator/health/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/error"
    };

    @Bean
    @ConditionalOnProperty(name = "readrace.auth.modo", havingValue = "seed")
    SecurityFilterChain semLogin(HttpSecurity http) throws Exception {
        return base(http).authorizeHttpRequests(a -> a.anyRequest().permitAll()).build();
    }

    @Bean
    @ConditionalOnProperty(name = "readrace.auth.modo", havingValue = "cognito")
    SecurityFilterChain comCognito(HttpSecurity http, ObjectMapper json) throws Exception {
        return base(http)
                .authorizeHttpRequests(
                        a -> a.requestMatchers(PUBLICOS).permitAll().anyRequest().authenticated())
                .oauth2ResourceServer(
                        o ->
                                o.authenticationEntryPoint(naoAutenticadoNoEnvelope(json))
                                        .jwt(Customizer.withDefaults()))
                .build();
    }

    /**
     * Sem token, ou com token inválido ou expirado, o Spring Security responde 401 antes do
     * controller, e o GlobalExceptionHandler não vê o erro. O padrão é um corpo vazio; aqui ele
     * ganha o mesmo envelope de erro do resto da API (#10). O cabeçalho WWW-Authenticate do Bearer
     * continua o do Spring.
     */
    private static AuthenticationEntryPoint naoAutenticadoNoEnvelope(ObjectMapper json) {
        BearerTokenAuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();
        return (request, response, excecao) -> {
            bearer.commence(request, response, excecao);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            json.writeValue(response.getOutputStream(), ErroResponse.de(CodigoErro.UNAUTHORIZED));
        };
    }

    /**
     * O access token do Cognito não tem "aud". Em vez disso, valida que é um access token
     * (token_use) emitido para o nosso app client (client_id).
     */
    @Bean
    @ConditionalOnProperty(name = "readrace.auth.modo", havingValue = "cognito")
    JwtDecoder jwtDecoder(
            @Value("${readrace.cognito.issuer-uri}") String issuer,
            @Value("${readrace.cognito.client-id}") String clientId) {
        Assert.hasText(issuer, "COGNITO_ISSUER_URI não configurado");
        Assert.hasText(clientId, "COGNITO_CLIENT_ID não configurado");

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer),
                        new JwtClaimValidator<String>("token_use", "access"::equals),
                        new JwtClaimValidator<String>("client_id", clientId::equals)));
        return decoder;
    }

    private HttpSecurity base(HttpSecurity http) throws Exception {
        return http.cors(Customizer.withDefaults()) // usa as regras do CorsConfig
                .csrf(csrf -> csrf.disable()) // API stateless com Bearer, sem cookie
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }
}
