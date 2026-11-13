package com.readrace.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.adapter.cognito.CognitoUserInfo;
import com.readrace.api.adapter.cognito.CognitoUserInfoClient;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.SeguirRepository;
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
    @Test
    void criacao_de_comunidade_exige_login_e_usa_o_administrador_do_token() {
        String corpo = "{\"nome\":\"Comunidade autenticada\",\"criadorId\":\"" + ID_DANIEL + "\"}";
        assertThat(
                        mvc.post()
                                .uri("/api/comunidades")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(corpo))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        UUID ana = UUID.fromString("00000000-0000-0000-0000-000000000002");
        jdbc.update("UPDATE usuario SET cognito_sub = 'sub-ana-cria-comunidade' WHERE id = ?", ana);
        assertThat(
                        mvc.post()
                                .uri("/api/comunidades")
                                .with(token("sub-ana-cria-comunidade", "token-ana-comunidade"))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(corpo))
                .hasStatus(HttpStatus.CREATED);
        assertThat(
                        mvc.get()
                                .uri("/api/feed/comunidades")
                                .with(token("sub-ana-cria-comunidade", "token-ana-comunidade")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.comunidades[*].nome")
                .asArray()
                .contains("Comunidade autenticada");
        assertThat(
                        jdbc.queryForList(
                                "SELECT mc.usuario_id, mc.cargo_comunidade::text AS cargo FROM membro_comunidade mc JOIN comunidade c ON c.id = mc.comunidade_id WHERE c.nome = 'Comunidade autenticada'"))
                .containsExactly(java.util.Map.of("usuario_id", ana, "cargo", "administrador"));
    }

    private static final String ID_DANIEL = "00000000-0000-0000-0000-000000000001";

    @Autowired private MockMvcTester mvc;

    @Autowired private UsuarioRepository usuarios;

    @MockitoBean private JwtDecoder jwtDecoder;

    @MockitoBean private CognitoUserInfoClient userInfoClient;

    @Autowired private SeguirRepository seguir;

    @Test
    void mutacoesDeSolicitacoesUsamSolicitanteEAdministradorDoToken() {
        String base = "/api/clubes/50000000-0000-0000-0000-000000000001/solicitacoes";
        String pedido = "78000000-0000-0000-0000-000000000001";
        for (String rota :
                new String[] {
                    base, base + "/" + pedido + "/aceitar", base + "/" + pedido + "/recusar"
                }) {
            assertThat(mvc.post().uri(rota)).hasStatus(HttpStatus.UNAUTHORIZED);
        }
        UUID pedro = UUID.fromString("00000000-0000-0000-0000-000000000009");
        UUID ana = UUID.fromString("00000000-0000-0000-0000-000000000002");
        jdbc.update("UPDATE usuario SET cognito_sub = 'sub-pedro-mutacao-234' WHERE id = ?", pedro);
        jdbc.update("UPDATE usuario SET cognito_sub = 'sub-ana-mutacao-234' WHERE id = ?", ana);
        jdbc.update(
                "UPDATE membro_clube SET cargo_clube = 'administrador' WHERE usuario_id = ? AND clube_id = '50000000-0000-0000-0000-000000000001'",
                ana);
        assertThat(mvc.post().uri(base).with(token("sub-pedro-mutacao-234", "token-pedro-234")))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(pedido);
        assertThat(
                        mvc.post()
                                .uri(base + "/" + pedido + "/aceitar")
                                .with(token("sub-pedro-mutacao-234", "token-pedro-234")))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(
                        mvc.post()
                                .uri(base + "/" + pedido + "/aceitar")
                                .with(token("sub-ana-mutacao-234", "token-ana-234")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.situacao")
                .isEqualTo("aceita");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT administrador_id FROM solicitacao_entrada_clube WHERE id = ?",
                                UUID.class,
                                UUID.fromString(pedido)))
                .isEqualTo(ana);
    }

    @Test
    void consultasDeClubesExigemLoginEUsamSituacaoDoUsuarioDoToken() {
        String clube = "50000000-0000-0000-0000-000000000001";
        for (String rota :
                new String[] {
                    "/api/clubes",
                    "/api/clubes/" + clube,
                    "/api/clubes/" + clube + "/solicitacoes",
                    "/api/clubes/" + clube + "/posts"
                }) {
            assertThat(mvc.get().uri(rota)).hasStatus(HttpStatus.UNAUTHORIZED);
        }
        jdbc.update(
                "UPDATE usuario SET cognito_sub = 'sub-pedro-234' WHERE id = ?",
                UUID.fromString("00000000-0000-0000-0000-000000000009"));
        assertThat(mvc.get().uri("/api/clubes").with(token("sub-pedro-234", "token-pedro-234")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.clubes[?(@.id == '" + clube + "')].situacao")
                .asList()
                .containsExactly("pendente");
        assertThat(
                        mvc.get()
                                .uri("/api/clubes/" + clube)
                                .with(token("sub-pedro-234", "token-pedro-234")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.situacao")
                .isEqualTo("pendente");
        assertThat(
                        mvc.get()
                                .uri("/api/clubes/" + clube + "/solicitacoes")
                                .with(token("sub-pedro-234", "token-pedro-234")))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("SO_LIDER_CRIA_QUIZ");
        assertThat(
                        mvc.get()
                                .uri("/api/clubes/" + clube + "/posts")
                                .with(token("sub-pedro-234", "token-pedro-234")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.podeEscrever")
                .isEqualTo(false);
    }

    @Test
    void administradorDoTokenPodeListarSolicitacoesSemDependerDoSeed() {
        UUID ana = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID clube = UUID.fromString("50000000-0000-0000-0000-000000000003");
        jdbc.update("UPDATE usuario SET cognito_sub = 'sub-ana-234' WHERE id = ?", ana);
        assertThat(
                        mvc.get()
                                .uri("/api/clubes/" + clube + "/solicitacoes")
                                .with(token("sub-ana-234", "token-ana-234")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.solicitacoes")
                .asList()
                .isEmpty();
        assertThat(
                        mvc.get()
                                .uri("/api/clubes/" + clube)
                                .with(token("sub-ana-234", "token-ana-234")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.solicitacoesPendentes")
                .isEqualTo(0);
    }

    @Test
    void buscaExcluiOUsuarioDoTokenEmVezDoUsuarioDeDemonstracao() {
        String idAna = "00000000-0000-0000-0000-000000000002";
        String sub = "sub-busca-222";
        jdbc.update(
                """
                UPDATE usuario
                SET nome = 'Daniel Ribeiro', cognito_sub = ?
                WHERE id = ?
                """,
                sub,
                UUID.fromString(idAna));

        assertThat(
                        mvc.get()
                                .uri("/api/busca")
                                .param("q", "Daniel Ribeiro")
                                .param("tipo", "usuarios")
                                .with(token(sub, "token-busca-222")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens[*].id")
                .asList()
                .contains(ID_DANIEL)
                .doesNotContain(idAna);
    }

    @Test
    void seguirUsaOUsuarioDoTokenEmVezDoUsuarioDeDemonstracao() {
        given(userInfoClient.buscar("token-seguir"))
                .willReturn(
                        new CognitoUserInfo(
                                "sub-seguir",
                                "leitora.seguir@example.com",
                                "true",
                                "Leitora",
                                null));
        String rota = "/api/usuarios/" + ID_DANIEL + "/seguidores/eu";
        assertThat(mvc.post().uri(rota)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.delete().uri(rota)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.post().uri(rota).with(token("sub-seguir", "token-seguir")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.sigo")
                .isEqualTo(true);
        var leitora = usuarios.findByCognitoSubAndExcluidoEmIsNull("sub-seguir").orElseThrow();
        assertThat(
                        seguir.existsBySeguidorIdAndSeguidoId(
                                leitora.getId(), UUID.fromString(ID_DANIEL)))
                .isTrue();
        assertThat(
                        mvc.get()
                                .uri("/api/usuarios/" + ID_DANIEL + "/perfil")
                                .with(token("sub-seguir", "token-seguir")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.sigo")
                .isEqualTo(true);
        assertThat(mvc.delete().uri(rota).with(token("sub-seguir", "token-seguir")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.sigo")
                .isEqualTo(false);
        assertThat(
                        seguir.existsBySeguidorIdAndSeguidoId(
                                leitora.getId(), UUID.fromString(ID_DANIEL)))
                .isFalse();
    }

    @Autowired private JdbcTemplate jdbc;

    /**
     * O primeiro acesso grava em transação própria (REQUIRES_NEW), que o rollback do teste não
     * desfaz. Depois do rollback, apaga os perfis criados aqui e desliga o Daniel do seed, para não
     * vazar estado para outros testes.
     */
    @AfterTransaction
    void limparPrimeirosAcessos() {
        jdbc.update(
                """
                DELETE FROM usuario WHERE cognito_sub IN (
                    'sub-novo', 'sub-dominio-real', 'sub-seguir', 'sub-sem-email',
                    'sub-email-em-branco', 'sub-nome-longo', 'sub-daniel-b', 'sub-nao-verificado')
                """);
        jdbc.update(
                "UPDATE usuario SET cognito_sub = NULL WHERE cognito_sub IN ('sub-daniel', 'sub-daniel-a')");
    }

    @Test
    void deve_recusar_request_sem_token() {
        var resposta = mvc.get().uri("/api/me").exchange();

        assertThat(resposta).headers().containsHeader("WWW-Authenticate");
        assertThat(resposta)
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("UNAUTHORIZED");
    }

    @Test
    void desafios_de_sistema_exigem_token() {
        assertThat(mvc.get().uri("/api/desafios/sistema"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("UNAUTHORIZED");
    }

    // O 401 do Spring Security, antes do controller, segue o mesmo envelope de erro da API.
    @Test
    void deve_recusar_token_invalido_no_envelope_de_erro() {
        given(jwtDecoder.decode("token-ruim"))
                .willThrow(new BadJwtException("assinatura inválida"));

        assertThat(mvc.get().uri("/api/me").header("Authorization", "Bearer token-ruim"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("UNAUTHORIZED");
    }

    // O 401 do primeiro acesso (Cognito recusou o token no GetUser) passa pelo
    // GlobalExceptionHandler, que precisa do código próprio em vez de INTERNAL_ERROR.
    @Test
    void deve_responder_401_no_envelope_quando_o_cognito_recusa_no_primeiro_acesso() {
        given(userInfoClient.buscar("token-recusado"))
                .willThrow(
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED, "Sessão inválida. Entre de novo."));

        assertThat(mvc.get().uri("/api/me").with(token("sub-novo", "token-recusado")))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("UNAUTHORIZED");
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
                                "sub-daniel", "daniel@example.com", "true", "Daniel", null));

        assertThat(mvc.get().uri("/api/me").with(token("sub-daniel", "token-daniel")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(ID_DANIEL);
    }

    @Test
    void nao_deve_vincular_pelo_email_antigo_do_seed() {
        // @readrace.com é domínio real de terceiros: o dono não pode assumir o perfil do seed.
        given(userInfoClient.buscar("token-dominio-real"))
                .willReturn(
                        new CognitoUserInfo(
                                "sub-dominio-real", "daniel@readrace.com", "true", "Daniel", null));

        assertThat(mvc.get().uri("/api/me").with(token("sub-dominio-real", "token-dominio-real")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.id")
                .isNotEqualTo(ID_DANIEL);
    }

    @Test
    void deve_criar_perfil_para_conta_sem_email() {
        given(userInfoClient.buscar("token-sem-email"))
                .willReturn(new CognitoUserInfo("sub-sem-email", null, null, null, null));

        assertThat(mvc.get().uri("/api/me").with(token("sub-sem-email", "token-sem-email")))
                .hasStatusOk();

        Usuario criado =
                usuarios.findByCognitoSubAndExcluidoEmIsNull("sub-sem-email").orElseThrow();
        assertThat(criado.getNomeUsuario()).startsWith("leitor");
        assertThat(criado.getNome()).isNotBlank();
        assertThat(criado.getEmail()).isEqualTo("sub-sem-email@sem-email.invalid");
    }

    @Test
    void deve_tratar_email_em_branco_como_ausente() {
        given(userInfoClient.buscar("token-em-branco"))
                .willReturn(new CognitoUserInfo("sub-email-em-branco", "  ", "true", "Ana", null));

        assertThat(mvc.get().uri("/api/me").with(token("sub-email-em-branco", "token-em-branco")))
                .hasStatusOk();

        Usuario criado =
                usuarios.findByCognitoSubAndExcluidoEmIsNull("sub-email-em-branco").orElseThrow();
        assertThat(criado.getNome()).isEqualTo("Ana");
        assertThat(criado.getEmail()).isEqualTo("sub-email-em-branco@sem-email.invalid");
    }

    @Test
    void deve_cortar_nome_com_mais_de_120_caracteres() {
        given(userInfoClient.buscar("token-nome-longo"))
                .willReturn(
                        new CognitoUserInfo(
                                "sub-nome-longo",
                                "nome.longo@teste.com",
                                "true",
                                "A".repeat(200),
                                null));

        assertThat(mvc.get().uri("/api/me").with(token("sub-nome-longo", "token-nome-longo")))
                .hasStatusOk();

        Usuario criado =
                usuarios.findByCognitoSubAndExcluidoEmIsNull("sub-nome-longo").orElseThrow();
        assertThat(criado.getNome()).hasSize(120);
    }

    @Test
    void nao_deve_tomar_o_perfil_ja_vinculado_a_outra_identidade() {
        given(userInfoClient.buscar("token-daniel-a"))
                .willReturn(
                        new CognitoUserInfo(
                                "sub-daniel-a", "daniel@example.com", "true", "Daniel", null));
        given(userInfoClient.buscar("token-daniel-b"))
                .willReturn(
                        new CognitoUserInfo(
                                "sub-daniel-b", "daniel@example.com", "true", "Outro", null));

        assertThat(mvc.get().uri("/api/me").with(token("sub-daniel-a", "token-daniel-a")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(ID_DANIEL);
        assertThat(mvc.get().uri("/api/me").with(token("sub-daniel-b", "token-daniel-b")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.id")
                .isNotEqualTo(ID_DANIEL);

        assertThat(
                        jdbc.queryForObject(
                                "SELECT cognito_sub FROM usuario WHERE id = ?",
                                String.class,
                                UUID.fromString(ID_DANIEL)))
                .isEqualTo("sub-daniel-a");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT email FROM usuario WHERE cognito_sub = 'sub-daniel-b'",
                                String.class))
                .isEqualTo("sub-daniel-b@sem-email.invalid");
    }

    @Test
    void deve_criar_perfil_novo_quando_o_email_ja_existe_mas_nao_foi_verificado() {
        given(userInfoClient.buscar("token-nao-verificado"))
                .willReturn(
                        new CognitoUserInfo(
                                "sub-nao-verificado",
                                "daniel@example.com",
                                "false",
                                "Alguém",
                                null));

        assertThat(
                        mvc.get()
                                .uri("/api/me")
                                .with(token("sub-nao-verificado", "token-nao-verificado")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.id")
                .isNotEqualTo(ID_DANIEL);

        assertThat(
                        jdbc.queryForObject(
                                "SELECT cognito_sub FROM usuario WHERE id = ?",
                                String.class,
                                UUID.fromString(ID_DANIEL)))
                .isNull();
    }

    private static RequestPostProcessor token(String sub, String valorDoToken) {
        return jwt().jwt(j -> j.subject(sub).tokenValue(valorDoToken));
    }
}
