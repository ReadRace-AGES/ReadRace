package com.readrace.api.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração de CORS (Cross-Origin Resource Sharing).
 *
 * <p>Permite que clientes de outras origens (mobile via Expo web, painel admin, etc.) façam
 * requests para a API sem serem bloqueados pelo navegador.
 *
 * <p>As origens vêm de {@code readrace.cors.allowed-origins} e o padrão é <b>vazio</b>. Isso é
 * intencional: sem configuração, a API fica fechada. O erro de esquecer de configurar aparece como
 * request bloqueado em homologação — não como API aberta para qualquer origem em produção.
 *
 * <p>Em dev, o {@code application-dev.yaml} libera {@code *}.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${readrace.cors.allowed-origins:}") List<String> allowedOrigins) {
        this.allowedOrigins =
                allowedOrigins.stream().map(String::trim).filter(o -> !o.isEmpty()).toList();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.isEmpty()) {
            log.info(
                    "CORS desabilitado: nenhuma origem em readrace.cors.allowed-origins."
                            + " Requests de outra origem serão bloqueados pelo navegador.");
            return;
        }

        log.info("CORS habilitado para as origens: {}", allowedOrigins);

        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Location")
                .maxAge(3600);
    }
}
