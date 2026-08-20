package com.readrace.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do Swagger/OpenAPI.
 *
 * <p>Personaliza as informações exibidas na interface do Swagger UI. A documentação fica disponível
 * em:
 *
 * <ul>
 *   <li>Swagger UI: <a href="http://localhost:8080/swagger-ui.html">/swagger-ui.html</a>
 *   <li>JSON da spec: <a href="http://localhost:8080/v3/api-docs">/v3/api-docs</a>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI readRaceOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("ReadRace API")
                                .description(
                                        "API do ReadRace - aplicativo de leitura gamificada."
                                            + " Projeto acadêmico PUCRS.")
                                .version("0.0.1-SNAPSHOT"));
    }
}
