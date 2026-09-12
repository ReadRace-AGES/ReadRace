package com.readrace.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

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

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Bean
    public OpenAPI readRaceOpenAPI() {
        String profileLabel = activeProfile.toUpperCase();
        String bookSource =
                "dev".equalsIgnoreCase(activeProfile)
                        ? "Mock local (books-seed.json)"
                        : "Google Books API (real)";

        return new OpenAPI()
                .info(
                        new Info()
                                .title("ReadRace API [" + profileLabel + "]")
                                .description(
                                        "API do ReadRace - aplicativo de leitura gamificada."
                                                + " Projeto acadêmico PUCRS."
                                                + "\n\n**Profile ativo:** "
                                                + profileLabel
                                                + "\n\n**Fonte de livros:** "
                                                + bookSource)
                                .version("0.0.1-SNAPSHOT"));
    }
}
