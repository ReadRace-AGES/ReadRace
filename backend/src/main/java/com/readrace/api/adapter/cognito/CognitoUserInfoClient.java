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
     * Token recusado pelo Cognito (4xx) vira 401: sessão antiga da página do Cognito, sem o escopo
     * aws.cognito.signin.user.admin, ou token revogado. Com 401 o app renova, perde a sessão e leva
     * a pessoa para a entrada; um 500 a deixava presa em erro. Falha do próprio Cognito (5xx) segue
     * como erro do servidor.
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
        } catch (HttpClientErrorException recusado) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Sessão inválida. Entre de novo.", recusado);
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
