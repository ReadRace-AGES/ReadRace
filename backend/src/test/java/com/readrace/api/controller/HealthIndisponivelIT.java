package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.service.HealthService;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GET /api/health com dependência fora")
class HealthIndisponivelIT {
    @MockitoBean private HealthService healthService;

    @Autowired private MockMvcTester mvc;

    @Test
    void deve_responder_503_quando_alguma_dependencia_estiver_fora() {
        given(healthService.apiEstaSaudavel()).willReturn(false);

        assertThat(mvc.get().uri("/api/health")).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void deve_dizer_down_no_corpo_quando_estiver_indisponivel() {
        given(healthService.apiEstaSaudavel()).willReturn(false);

        assertThat(mvc.get().uri("/api/health"))
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("down");
    }

    @Test
    void deve_voltar_a_responder_200_quando_a_dependencia_se_recuperar() {
        given(healthService.apiEstaSaudavel()).willReturn(true);

        assertThat(mvc.get().uri("/api/health"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("ok");
    }
}
