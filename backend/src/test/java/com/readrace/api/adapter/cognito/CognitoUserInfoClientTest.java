package com.readrace.api.adapter.cognito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * Testa a leitura da resposta do GetUser e o endereço da API, sem rede. O client só sobe com
 * readrace.auth.modo=cognito.
 */
@DisplayName("CognitoUserInfoClient — GetUser")
class CognitoUserInfoClientTest {

    private static final String ISSUER =
            "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_MEVIpejhy";
    private static final String API = "https://cognito-idp.us-east-2.amazonaws.com/";
    private static final MediaType AMZ_JSON = MediaType.valueOf("application/x-amz-json-1.1");

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
    void chama_o_get_user_com_o_token_da_pessoa() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer cognito = MockRestServiceServer.bindTo(builder).build();
        cognito.expect(requestTo(API))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Amz-Target", "AWSCognitoIdentityProviderService.GetUser"))
                .andExpect(content().json("{\"AccessToken\":\"token-da-pessoa\"}"))
                .andRespond(withSuccess(RESPOSTA, AMZ_JSON));

        CognitoUserInfo info = new CognitoUserInfoClient(builder, ISSUER).buscar("token-da-pessoa");

        assertThat(info.email()).isEqualTo("leitora@example.com");
        cognito.verify();
    }

    // Sessão antiga da página do Cognito (sem o escopo aws.cognito.signin.user.admin) ou token
    // revogado: o GetUser recusa com 400. Tem que virar 401 para o app renovar, perder a sessão e
    // levar a pessoa para a entrada; um 500 deixava o app preso em erro.
    @Test
    void token_recusado_pelo_cognito_vira_401() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer cognito = MockRestServiceServer.bindTo(builder).build();
        cognito.expect(requestTo(API))
                .andRespond(
                        withStatus(HttpStatus.BAD_REQUEST)
                                .contentType(AMZ_JSON)
                                .body(
                                        "{\"__type\":\"NotAuthorizedException\","
                                                + "\"message\":\"Access Token does not have required scopes\"}"));

        CognitoUserInfoClient client = new CognitoUserInfoClient(builder, ISSUER);

        assertThatThrownBy(() -> client.buscar("token-antigo"))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void endereco_da_api_sai_do_issuer() {
        assertThat(
                        CognitoUserInfoClient.enderecoDaApi(
                                "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_MEVIpejhy"))
                .isEqualTo("https://cognito-idp.us-east-2.amazonaws.com/");
    }
}
