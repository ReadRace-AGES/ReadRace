package com.readrace.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.adapter.cognito.CognitoUserInfo;
import com.readrace.api.adapter.cognito.CognitoUserInfoClient;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.UsuarioRepository;

/**
 * Modo cognito testado sem AWS: o {@code jwt()} do spring-security-test injeta a autenticação
 * direto no request, então as chaves do Cognito nunca são buscadas, e o userInfo é mockado.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "readrace.auth.modo=cognito")
@AutoConfigureMockMvc
@Transactional
@DisplayName("Autenticação com Cognito")
class AutenticacaoCognitoIT {
    private static final String ID_DANIEL = "00000000-0000-0000-0000-000000000001";

    @Autowired private MockMvcTester mvc;

    @Autowired private UsuarioRepository usuarios;

    @MockitoBean private JwtDecoder jwtDecoder;

    @MockitoBean private CognitoUserInfoClient userInfoClient;

    @Test
    void deve_recusar_request_sem_token() {
        assertThat(mvc.get().uri("/api/me")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deve_liberar_health_sem_token() {
        assertThat(mvc.get().uri("/api/health")).hasStatusOk();
    }

    @Test
    void deve_criar_usuario_no_primeiro_acesso_e_reaproveitar_nos_seguintes() {
        given(userInfoClient.buscar("token-novo"))
                .willReturn(
                        new CognitoUserInfo(
                                "sub-novo", "novo.leitor@teste.com", "true", "Leitor Novo", null));

        assertThat(mvc.get().uri("/api/me").with(token("sub-novo", "token-novo"))).hasStatusOk();
        assertThat(mvc.get().uri("/api/me").with(token("sub-novo", "token-novo"))).hasStatusOk();

        Usuario criado = usuarios.findByCognitoSubAndExcluidoEmIsNull("sub-novo").orElseThrow();
        assertThat(criado.getNome()).isEqualTo("Leitor Novo");
        assertThat(criado.getNomeUsuario()).isEqualTo("novoleitor");
        then(userInfoClient).should(times(1)).buscar(any());
    }

    @Test
    void deve_vincular_usuario_existente_pelo_email_verificado() {
        given(userInfoClient.buscar("token-daniel"))
                .willReturn(
                        new CognitoUserInfo(
                                "sub-daniel", "daniel@readrace.com", "true", "Daniel", null));

        assertThat(mvc.get().uri("/api/me").with(token("sub-daniel", "token-daniel")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(ID_DANIEL);
    }

    private static RequestPostProcessor token(String sub, String valorDoToken) {
        return jwt().jwt(j -> j.subject(sub).tokenValue(valorDoToken));
    }
}
