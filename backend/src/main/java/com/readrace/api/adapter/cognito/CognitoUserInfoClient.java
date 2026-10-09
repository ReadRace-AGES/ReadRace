package com.readrace.api.adapter.cognito;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Busca nome, e-mail e foto no Cognito pelo GetUser, com o token da própria pessoa. O access token
 * só traz o "sub"; os dados de perfil vêm daqui, e não do app, porque o que o cliente envia não é
 * confiável.
 *
 * <p>GetUser, e não /oauth2/userInfo: o token do login por e-mail e senha no app traz o escopo
 * aws.cognito.signin.user.admin, e não openid, e o userInfo o recusa (#156). O Cognito responde com
 * application/x-amz-json-1.1, que o conversor JSON do RestClient não reconhece; por isso o corpo
 * trafega como String.
 */
@Component
@ConditionalOnProperty(name = "readrace.auth.modo", havingValue = "cognito")
public class CognitoUserInfoClient {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final MediaType AMZ_JSON = MediaType.valueOf("application/x-amz-json-1.1");

    private final RestClient restClient;

    @Autowired
    public CognitoUserInfoClient(@Value("${readrace.cognito.issuer-uri}") String issuerUri) {
        this(RestClient.builder(), issuerUri);
    }

    /** Recebe o builder para os testes simularem o Cognito com MockRestServiceServer. */
    CognitoUserInfoClient(RestClient.Builder builder, String issuerUri) {
        this.restClient = builder.baseUrl(enderecoDaApi(issuerUri)).build();
    }

    /**
     * Só o token recusado vira 401: sessão antiga da página do Cognito, sem o escopo
     * aws.cognito.signin.user.admin, token revogado ou conta apagada. Com 401 o app renova, perde a
     * sessão e leva a pessoa para a entrada; um 500 a deixava presa em erro.
     *
     * <p>O Cognito responde 4xx também para o limite de chamadas. Esse vira 503: como 401, o app
     * renovaria, bateria no mesmo limite e apagaria uma sessão válida. Outro 4xx é defeito da nossa
     * chamada e vira 500. Falha do próprio Cognito (5xx) segue como erro do servidor.
     */
    public CognitoUserInfo buscar(String accessToken) {
        try {
            String resposta =
                    restClient
                            .post()
                            .uri("/")
                            .contentType(AMZ_JSON)
                            .header("X-Amz-Target", "AWSCognitoIdentityProviderService.GetUser")
                            .body(JSON.writeValueAsString(Map.of("AccessToken", accessToken)))
                            .retrieve()
                            .body(String.class);
            return lerResposta(resposta);
        } catch (HttpClientErrorException erro) {
            throw traduzirErro(erro);
        }
    }

    private static ResponseStatusException traduzirErro(HttpClientErrorException erro) {
        return switch (tipoDoErro(erro.getResponseBodyAsString())) {
            case "NotAuthorizedException", "UserNotFoundException" ->
                    new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Sessão inválida. Entre de novo.", erro);
            case "TooManyRequestsException", "LimitExceededException" ->
                    new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "Serviço de login ocupado. Tente de novo em instantes.",
                            erro);
            default ->
                    new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao consultar o login.", erro);
        };
    }

    /** O Cognito manda o tipo em "__type", às vezes com prefixo ("...#NotAuthorizedException"). */
    static String tipoDoErro(String corpo) {
        try {
            String tipo = JSON.readTree(corpo).path("__type").asString("");
            return tipo.substring(tipo.lastIndexOf('#') + 1);
        } catch (RuntimeException corpoNaoJson) {
            return "";
        }
    }

    /** O issuer é https://cognito-idp.<região>.amazonaws.com/<pool>; a API fica na raiz do host. */
    static String enderecoDaApi(String issuerUri) {
        URI issuer = URI.create(issuerUri);
        return issuer.getScheme() + "://" + issuer.getHost() + "/";
    }

    static CognitoUserInfo lerResposta(String json) {
        Map<String, String> atributos = new HashMap<>();
        for (JsonNode atributo : JSON.readTree(json).path("UserAttributes")) {
            atributos.put(atributo.path("Name").asString(), atributo.path("Value").asString());
        }
        return new CognitoUserInfo(
                atributos.get("sub"),
                atributos.get("email"),
                atributos.get("email_verified"),
                atributos.get("name"),
                atributos.get("picture"));
    }
}
