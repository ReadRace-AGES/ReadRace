package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.readrace.api.TestcontainersConfiguration;

/**
 * Prova que a lista de origens do {@code application.yaml} é realmente respeitada — e que origem
 * fora da lista não recebe liberação.
 *
 * <p>Contexto separado porque a propriedade de CORS é lida na criação do bean.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "readrace.cors.allowed-origins=https://app.readrace.com")
@AutoConfigureMockMvc
@DisplayName("CORS com origens configuradas")
class CorsConfiguradoIT {

    @Autowired private MockMvcTester mvc;

    @Test
    void deve_liberar_a_origem_que_esta_na_lista() {
        assertThat(mvc.get().uri("/api/exemplos").header("Origin", "https://app.readrace.com"))
                .hasStatusOk()
                .hasHeader("Access-Control-Allow-Origin", "https://app.readrace.com");
    }

    @Test
    void nao_deve_liberar_origem_fora_da_lista() {
        assertThat(mvc.get().uri("/api/exemplos").header("Origin", "https://site-invasor.example"))
                .doesNotContainHeader("Access-Control-Allow-Origin");
    }
}
