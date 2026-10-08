package com.readrace.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.adapter.cognito.CognitoUserInfo;
import com.readrace.api.adapter.cognito.CognitoUserInfoClient;

/**
 * Primeiro acesso por uma rota de leitura, como o app faz ao abrir as abas (#156).
 *
 * <p>Sem @Transactional na classe de propósito: a transação de escrita do teste esconderia a
 * transação somente leitura do service, que era onde o perfil criado no primeiro acesso nunca
 * chegava ao banco e a resposta saía "Usuário não encontrado". A limpeza fica no @AfterEach.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "readrace.auth.modo=cognito")
@AutoConfigureMockMvc
@DisplayName("Primeiro acesso com Cognito por rota de leitura")
class PrimeiroAcessoCognitoIT {
    private static final String SUB = "sub-primeiro-acesso-perfil";

    @Autowired private MockMvcTester mvc;

    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private JwtDecoder jwtDecoder;

    @MockitoBean private CognitoUserInfoClient userInfoClient;

    @AfterEach
    void limpar() {
        jdbc.update("DELETE FROM usuario WHERE cognito_sub = ?", SUB);
    }

    @Test
    void deve_criar_o_perfil_no_primeiro_acesso_pela_rota_do_perfil() {
        given(userInfoClient.buscar("token-perfil"))
                .willReturn(
                        new CognitoUserInfo(
                                SUB, "leitora.perfil@teste.com", "true", "Leitora Perfil", null));

        assertThat(
                        mvc.get()
                                .uri("/api/me/perfil")
                                .with(jwt().jwt(j -> j.subject(SUB).tokenValue("token-perfil"))))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.nome")
                .isEqualTo("Leitora Perfil");

        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM usuario WHERE cognito_sub = ?",
                                Integer.class,
                                SUB))
                .isEqualTo(1);
    }
}
