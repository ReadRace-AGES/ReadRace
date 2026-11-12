package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.model.UsuarioId;
import com.readrace.api.service.ConquistaService;
import com.readrace.api.service.UsuarioAtual;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class SolicitacaoClubeControllerIT {
    private static final UUID CLUBE = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID CLUBE_2 = UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final UUID ADMIN = usuario(1);
    private static final UUID PEDRO = usuario(9);
    private static final UUID PEDIDO = UUID.fromString("78000000-0000-0000-0000-000000000001");
    @Autowired private MockMvcTester mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;
    @MockitoSpyBean private UsuarioAtual usuarioAtual;
    @MockitoSpyBean private ConquistaService conquistas;

    @Test
    void pede_uma_vez_sem_associacao_pontos_ou_recompensa() throws Exception {
        UUID clube = UUID.fromString("50000000-0000-0000-0000-000000000004");
        como(ADMIN);
        var antes = gamificacao(ADMIN);
        Map<String, Object> primeiro = post(base(clube), HttpStatus.CREATED);
        assertThat(primeiro)
                .containsOnlyKeys("id", "situacao")
                .containsEntry("situacao", "pendente");
        assertThat(post(base(clube), HttpStatus.CREATED)).isEqualTo(primeiro);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM solicitacao_entrada_clube WHERE clube_id = ? AND usuario_id = ?",
                                Integer.class,
                                clube,
                                ADMIN))
                .isEqualTo(1);
        assertThat(gamificacao(ADMIN)).isEqualTo(antes);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void associado_nao_pede(int numero) throws Exception {
        como(usuario(numero));
        int antes = totalPedidos();
        erro(base(CLUBE), HttpStatus.CONFLICT, "JA_E_MEMBRO");
        assertThat(totalPedidos()).isEqualTo(antes);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void clube_ausente_ou_excluido(boolean excluido) throws Exception {
        UUID clube = excluido ? CLUBE : UUID.randomUUID();
        if (excluido)
            jdbc.update("UPDATE clube_do_livro SET excluido_em = now() WHERE id = ?", clube);
        como(PEDRO);
        erro(base(clube), HttpStatus.NOT_FOUND, "CLUBE_NAO_ENCONTRADO");
        como(ADMIN);
        for (String acao : List.of("aceitar", "recusar")) {
            erro(
                    base(clube) + "/" + PEDIDO + "/" + acao,
                    HttpStatus.NOT_FOUND,
                    "CLUBE_NAO_ENCONTRADO");
        }
    }

    @Test
    void recusa_preserva_dados_e_permite_outro_pedido() throws Exception {
        como(ADMIN);
        var antes = gamificacao(PEDRO);
        assertThat(post(acao(PEDIDO, "recusar"), HttpStatus.OK))
                .containsEntry("situacao", "recusada");
        assertThat(gamificacao(PEDRO)).isEqualTo(antes);
        como(PEDRO);
        var novo = post(base(CLUBE), HttpStatus.CREATED);
        assertThat(novo.get("id")).isNotEqualTo(PEDIDO.toString());
        em.flush();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM solicitacao_entrada_clube WHERE clube_id = ? AND usuario_id = ?",
                                Integer.class,
                                CLUBE,
                                PEDRO))
                .isEqualTo(2);
        assertThat(
                        jdbc.queryForMap(
                                "SELECT situacao::text AS situacao, administrador_id FROM solicitacao_entrada_clube WHERE id = ?",
                                PEDIDO))
                .containsEntry("situacao", "recusada")
                .containsEntry("administrador_id", ADMIN);
    }

    @ParameterizedTest
    @CsvSource({"2,aceitar", "2,recusar", "9,aceitar", "9,recusar"})
    void apenas_administrador_atual_responde(int numero, String acao) throws Exception {
        como(usuario(numero));
        var antes = gamificacao(PEDRO);
        erro(acao(PEDIDO, acao), HttpStatus.FORBIDDEN, "SO_LIDER_CRIA_QUIZ");
        assertThat(gamificacao(PEDRO)).isEqualTo(antes);
        assertThat(situacao(PEDIDO)).isEqualTo("pendente");
    }

    @ParameterizedTest
    @ValueSource(strings = {"aceitar", "recusar"})
    void id_inexistente_ou_de_outro_clube(String acao) throws Exception {
        como(ADMIN);
        erro(acao(UUID.randomUUID(), acao), HttpStatus.NOT_FOUND, "SOLICITACAO_NAO_ENCONTRADA");
        UUID outro = UUID.fromString("78000000-0000-0000-0000-000000000003");
        erro(acao(outro, acao), HttpStatus.NOT_FOUND, "SOLICITACAO_NAO_ENCONTRADA");
        assertThat(situacao(outro)).isEqualTo("pendente");
    }

    @ParameterizedTest
    @ValueSource(strings = {"aceitar", "recusar"})
    void resposta_nao_pode_ser_sobrescrita(String primeiraAcao) throws Exception {
        como(ADMIN);
        post(acao(PEDIDO, primeiraAcao), HttpStatus.OK);
        em.flush();
        var antes =
                jdbc.queryForMap("SELECT * FROM solicitacao_entrada_clube WHERE id = ?", PEDIDO);
        for (String repetida : List.of("aceitar", "recusar")) {
            erro(acao(PEDIDO, repetida), HttpStatus.CONFLICT, "SOLICITACAO_JA_RESPONDIDA");
        }
        assertThat(jdbc.queryForMap("SELECT * FROM solicitacao_entrada_clube WHERE id = ?", PEDIDO))
                .isEqualTo(antes);
    }

    @Test
    void aceite_cria_membro_zero_pontos_ranking_feed_e_conquista_uma_vez() throws Exception {
        UUID novo = novoUsuario();
        como(novo);
        UUID pedido = UUID.fromString((String) post(base(CLUBE), HttpStatus.CREATED).get("id"));
        como(ADMIN);
        assertThat(post(acao(pedido, "aceitar"), HttpStatus.OK))
                .containsEntry("id", pedido.toString())
                .containsEntry("situacao", "aceita");
        em.flush();
        assertThat(
                        jdbc.queryForMap(
                                "SELECT pontos, cargo_clube::text AS cargo FROM membro_clube WHERE clube_id = ? AND usuario_id = ?",
                                CLUBE,
                                novo))
                .containsEntry("pontos", 0)
                .containsEntry("cargo", "membro");
        assertThat(
                        jdbc.queryForMap(
                                "SELECT administrador_id, respondida_em FROM solicitacao_entrada_clube WHERE id = ?",
                                pedido))
                .containsEntry("administrador_id", ADMIN)
                .doesNotContainValue(null);
        assertThat(xp(novo)).isEqualTo(75);
        assertThat(totalConquistas(novo)).isEqualTo(1);
        como(novo);
        assertThat(mvc.get().uri("/api/feed/comunidades"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.clubes[*].id")
                .asList()
                .contains(CLUBE.toString());
        assertThat(mvc.get().uri("/api/clubes/" + CLUBE + "/ranking"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.ranking[?(@.usuario.id == '" + novo + "')].pontos")
                .asList()
                .containsExactly(0);
        UUID segundo = UUID.fromString((String) post(base(CLUBE_2), HttpStatus.CREATED).get("id"));
        como(ADMIN);
        post(base(CLUBE_2) + "/" + segundo + "/aceitar", HttpStatus.OK);
        em.flush();
        assertThat(xp(novo)).isEqualTo(75);
        assertThat(totalConquistas(novo)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"membro", "administrador"})
    void aceite_preserva_associacao_preexistente(String cargo) throws Exception {
        UUID membro = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO membro_clube (id, clube_id, usuario_id, cargo_clube, pontos) VALUES (?, ?, ?, ?::cargo, 321)",
                membro,
                CLUBE,
                PEDRO,
                cargo);
        como(ADMIN);
        post(acao(PEDIDO, "aceitar"), HttpStatus.OK);
        em.flush();
        assertThat(
                        jdbc.queryForMap(
                                "SELECT id, cargo_clube::text AS cargo, pontos FROM membro_clube WHERE clube_id = ? AND usuario_id = ?",
                                CLUBE,
                                PEDRO))
                .containsEntry("id", membro)
                .containsEntry("cargo", cargo)
                .containsEntry("pontos", 321);
    }

    @Test
    void sucessor_responde_e_antigo_lider_nao() throws Exception {
        como(ADMIN);
        assertThat(mvc.delete().uri("/api/clubes/" + CLUBE + "/membros/eu")).hasStatusOk();
        erro(acao(PEDIDO, "aceitar"), HttpStatus.FORBIDDEN, "SO_LIDER_CRIA_QUIZ");
        como(usuario(2));
        post(acao(PEDIDO, "aceitar"), HttpStatus.OK);
        em.flush();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT administrador_id FROM solicitacao_entrada_clube WHERE id = ?",
                                UUID.class,
                                PEDIDO))
                .isEqualTo(usuario(2));
    }

    @Test
    void excluido_nao_entra_e_pedido_permanece_pendente() throws Exception {
        jdbc.update("UPDATE usuario SET excluido_em = now() WHERE id = ?", PEDRO);
        como(ADMIN);
        var antes = gamificacao(PEDRO);
        erro(acao(PEDIDO, "aceitar"), HttpStatus.NOT_FOUND, "USUARIO_NAO_ENCONTRADO");
        assertThat(situacao(PEDIDO)).isEqualTo("pendente");
        assertThat(gamificacao(PEDRO)).isEqualTo(antes);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void falha_depois_da_conquista_reverte_tudo_em_transacao_real() throws Exception {
        UUID novo = novoUsuario();
        try {
            como(novo);
            UUID pedido = UUID.fromString((String) post(base(CLUBE), HttpStatus.CREATED).get("id"));
            doAnswer(
                            invocacao -> {
                                invocacao.callRealMethod();
                                // Comprova que houve gravação real antes da falha, não só
                                // alterações em memória.
                                em.flush();
                                assertThat(totalConquistas(novo)).isEqualTo(1);
                                assertThat(xp(novo)).isEqualTo(75);
                                throw new IllegalStateException("Falha induzida após recompensa");
                            })
                    .when(conquistas)
                    .avaliar(novo);
            como(ADMIN);
            erro(acao(pedido, "aceitar"), HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
            assertThat(situacao(pedido)).isEqualTo("pendente");
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM membro_clube WHERE usuario_id = ?",
                                    Integer.class,
                                    novo))
                    .isZero();
            assertThat(totalConquistas(novo)).isZero();
            assertThat(xp(novo)).isZero();
            assertThat(
                            jdbc.queryForMap(
                                    "SELECT respondida_em, administrador_id FROM solicitacao_entrada_clube WHERE id = ?",
                                    pedido))
                    .containsEntry("respondida_em", null)
                    .containsEntry("administrador_id", null);
        } finally {
            jdbc.update("DELETE FROM solicitacao_entrada_clube WHERE usuario_id = ?", novo);
            jdbc.update("DELETE FROM membro_clube WHERE usuario_id = ?", novo);
            jdbc.update("DELETE FROM conquista_usuario WHERE usuario_id = ?", novo);
            jdbc.update("DELETE FROM usuario WHERE id = ?", novo);
        }
    }

    private UUID novoUsuario() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO usuario (id, nome, nome_usuario, email) VALUES (?, 'Novo leitor', ?, ?)",
                id,
                id.toString(),
                id + "@example.com");
        return id;
    }

    private Map<String, Object> gamificacao(UUID id) {
        return Map.of(
                "usuario",
                jdbc.queryForMap("SELECT xp_total, nivel FROM usuario WHERE id = ?", id),
                "membros",
                jdbc.queryForList(
                        "SELECT * FROM membro_clube WHERE usuario_id = ? ORDER BY id", id),
                "conquistas",
                jdbc.queryForList(
                        "SELECT * FROM conquista_usuario WHERE usuario_id = ? ORDER BY id", id));
    }

    private int xp(UUID id) {
        return jdbc.queryForObject("SELECT xp_total FROM usuario WHERE id = ?", Integer.class, id);
    }

    private int totalConquistas(UUID id) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM conquista_usuario WHERE usuario_id = ?", Integer.class, id);
    }

    private int totalPedidos() {
        return jdbc.queryForObject("SELECT count(*) FROM solicitacao_entrada_clube", Integer.class);
    }

    private String situacao(UUID id) {
        return jdbc.queryForObject(
                "SELECT situacao::text FROM solicitacao_entrada_clube WHERE id = ?",
                String.class,
                id);
    }

    private void como(UUID id) {
        doReturn(new UsuarioId(id)).when(usuarioAtual).idDoUsuarioAtual();
    }

    private static UUID usuario(int numero) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(numero));
    }

    private static String base(UUID clube) {
        return "/api/clubes/" + clube + "/solicitacoes";
    }

    private static String acao(UUID id, String acao) {
        return base(CLUBE) + "/" + id + "/" + acao;
    }

    private void erro(String rota, HttpStatus status, String codigo) throws Exception {
        assertThat(post(rota, status)).containsEntry("code", codigo).containsKeys("message");
    }

    private Map<String, Object> post(String rota, HttpStatus status) throws Exception {
        var resposta = mvc.post().uri(rota).exchange();
        assertThat(resposta).hasStatus(status);
        return JsonPath.read(resposta.getResponse().getContentAsString(), "$");
    }
}
