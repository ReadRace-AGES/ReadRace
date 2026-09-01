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
 * O healthcheck público da API, exigido pela issue #10.
 *
 * <p>Não confundir com o {@code /actuator/health}: aquele é infraestrutura, checa o banco e é usado
 * pelo docker-compose. Este é contrato de API — o mobile pergunta "a API está de pé?" e recebe
 * sempre o mesmo JSON, sem depender do formato do Actuator.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GET /api/health")
class HealthControllerIT {

    @Autowired private MockMvcTester mvc;

    @Test
    void deve_responder_200_com_status_ok() {
        assertThat(mvc.get().uri("/api/health"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("ok");
    }
}
