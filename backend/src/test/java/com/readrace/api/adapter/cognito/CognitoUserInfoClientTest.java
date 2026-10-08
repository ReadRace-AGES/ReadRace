package com.readrace.api.adapter.cognito;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testa a leitura da resposta do GetUser e o endereço da API, sem rede. O client só sobe com
 * readrace.auth.modo=cognito.
 */
@DisplayName("CognitoUserInfoClient — GetUser")
class CognitoUserInfoClientTest {

    private static final String RESPOSTA =
            """
            {"Username":"abc-123","UserAttributes":[
              {"Name":"sub","Value":"abc-123"},
              {"Name":"email","Value":"leitora@example.com"},
              {"Name":"email_verified","Value":"true"},
              {"Name":"name","Value":"Leitora Teste"},
              {"Name":"picture","Value":"https://img/x.png"}]}
            """;

    @Test
    void le_os_atributos_do_get_user() {
        CognitoUserInfo info = CognitoUserInfoClient.lerResposta(RESPOSTA);

        assertThat(info.sub()).isEqualTo("abc-123");
        assertThat(info.email()).isEqualTo("leitora@example.com");
        assertThat(info.emailVerificado()).isTrue();
        assertThat(info.name()).isEqualTo("Leitora Teste");
        assertThat(info.picture()).isEqualTo("https://img/x.png");
    }

    @Test
    void atributos_ausentes_viram_nulos_e_email_nao_verificado() {
        CognitoUserInfo info =
                CognitoUserInfoClient.lerResposta(
                        """
                        {"UserAttributes":[{"Name":"email","Value":"a@example.com"}]}
                        """);

        assertThat(info.email()).isEqualTo("a@example.com");
        assertThat(info.name()).isNull();
        assertThat(info.emailVerificado()).isFalse();
    }

    @Test
    void endereco_da_api_sai_do_issuer() {
        assertThat(
                        CognitoUserInfoClient.enderecoDaApi(
                                "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_MEVIpejhy"))
                .isEqualTo("https://cognito-idp.us-east-2.amazonaws.com/");
    }
}
