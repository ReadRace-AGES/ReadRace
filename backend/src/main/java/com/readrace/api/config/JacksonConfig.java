package com.readrace.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registra o {@link ObjectMapper} do Jackson 3 ({@code tools.jackson}) — o mesmo tipo que o Spring
 * Boot 4.1 usa para serializar os endpoints.
 *
 * <p>Importante: o Boot 4.1 migrou para o Jackson 3. O pacote antigo {@code
 * com.fasterxml.jackson.databind} (Jackson 2) só aparece no classpath por transitividade do
 * springdoc/swagger-core e NÃO participa da serialização dos controllers. Configurar um mapper
 * Jackson 2 aqui não teria efeito sobre a API e ainda sumiria numa eventual troca de versão do
 * springdoc. Por isso este bean é do Jackson 3.
 *
 * <p>No Jackson 3 o mapper é imutável e configurado via {@link JsonMapper#builder()}.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                // Ignora campos desconhecidos no JSON (ex.: seed ou respostas do Google com
                // campos a mais). No Jackson 3 já é o padrão, mas deixamos explícito por clareza.
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }
}
