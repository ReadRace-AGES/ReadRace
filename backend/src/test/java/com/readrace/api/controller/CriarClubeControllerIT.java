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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.dto.request.CriarClubeRequest;
import com.readrace.api.model.UsuarioId;
import com.readrace.api.service.ConquistaService;
import com.readrace.api.service.UsuarioAtual;

import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CriarClubeControllerIT {
    private static final UUID CRIADOR = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MEMBRO = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OUTRO = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID LIVRO = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Autowired private MockMvcTester mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private ObjectMapper mapper;
    @MockitoSpyBean private UsuarioAtual usuarioAtual;
    @MockitoSpyBean private ConquistaService conquistaService;

    @Test
    void deve_criar_com_livro_descricao_administrador_e_membros_sem_exigir_amizade() {
        jdbc.update("DELETE FROM seguir WHERE seguidor_id = ? OR seguido_id = ?", CRIADOR, CRIADOR);
        UUID clube = criar("Leitura em grupo", "Descrição", List.of(MEMBRO, OUTRO));

        assertThat(
                        jdbc.queryForMap(
                                "SELECT nome, descricao, livro_id, capa_url FROM clube_do_livro WHERE id = ?",
                                clube))
                .containsEntry("nome", "Leitura em grupo")
                .containsEntry("descricao", "Descrição")
                .containsEntry("livro_id", LIVRO)
                .containsEntry("capa_url", capa());
        List<Map<String, Object>> membros =
                jdbc.queryForList(
                        "SELECT usuario_id, cargo_clube::text AS cargo, pontos FROM membro_clube WHERE clube_id = ?",
                        clube);
        assertThat(membros)
                .containsExactlyInAnyOrder(
                        Map.of("usuario_id", CRIADOR, "cargo", "administrador", "pontos", 0),
                        Map.of("usuario_id", MEMBRO, "cargo", "membro", "pontos", 0),
                        Map.of("usuario_id", OUTRO, "cargo", "membro", "pontos", 0));

        for (UUID participante : List.of(CRIADOR, MEMBRO, OUTRO)) {
            doReturn(new UsuarioId(participante)).when(usuarioAtual).idDoUsuarioAtual();
            assertThat(mvc.get().uri("/api/feed/comunidades"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.clubes[*].id")
                    .asArray()
                    .contains(clube.toString());
        }
    }

    @Test
    void deve_aceitar_lista_vazia_e_descricao_opcional() {
        UUID clube = criar("Solo", null, List.of());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM membro_clube WHERE clube_id = ?",
                                Integer.class,
                                clube))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT cargo_clube::text FROM membro_clube WHERE clube_id = ?",
                                String.class,
                                clube))
                .isEqualTo("administrador");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT descricao FROM clube_do_livro WHERE id = ?",
                                String.class,
                                clube))
                .isNull();
    }

    @Test
    void deve_eliminar_ids_repetidos_e_ignorar_criador_na_lista() {
        UUID clube = criar("Sem duplicatas", null, List.of(MEMBRO, CRIADOR, MEMBRO, CRIADOR));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM membro_clube WHERE clube_id = ?",
                                Integer.class,
                                clube))
                .isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT cargo_clube::text FROM membro_clube WHERE clube_id = ? AND usuario_id = ?",
                                String.class,
                                clube,
                                CRIADOR))
                .isEqualTo("administrador");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void deve_rejeitar_nome_ausente_vazio_ou_em_branco(String nome) {
        erro(
                new CriarClubeRequest(nome, null, LIVRO, List.of()),
                HttpStatus.BAD_REQUEST,
                "CLUBE_INVALIDO");
    }

    @Test
    void deve_rejeitar_nome_acima_do_limite() {
        erro(
                new CriarClubeRequest("a".repeat(121), null, LIVRO, List.of()),
                HttpStatus.BAD_REQUEST,
                "CLUBE_INVALIDO");
    }

    @Test
    void deve_aceitar_nome_no_limite() {
        criar("a".repeat(120), null, List.of());
    }

    @Test
    void deve_rejeitar_livro_inexistente() {
        erro(
                new CriarClubeRequest("Clube", null, UUID.randomUUID(), List.of()),
                HttpStatus.NOT_FOUND,
                "LIVRO_NAO_ENCONTRADO");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deve_rejeitar_usuario_inexistente_sem_cadastro_parcial() {
        erro(
                new CriarClubeRequest("Clube", null, LIVRO, List.of(MEMBRO, UUID.randomUUID())),
                HttpStatus.NOT_FOUND,
                "USUARIO_NAO_ENCONTRADO");
    }

    @Test
    void deve_rejeitar_usuario_excluido() {
        jdbc.update("UPDATE usuario SET excluido_em = now() WHERE id = ?", MEMBRO);
        erro(
                new CriarClubeRequest("Clube", null, LIVRO, List.of(MEMBRO)),
                HttpStatus.NOT_FOUND,
                "USUARIO_NAO_ENCONTRADO");
    }

    @Test
    void deve_rejeitar_criador_excluido() {
        jdbc.update("UPDATE usuario SET excluido_em = now() WHERE id = ?", CRIADOR);
        erro(
                new CriarClubeRequest("Clube", null, LIVRO, List.of()),
                HttpStatus.NOT_FOUND,
                "USUARIO_NAO_ENCONTRADO");
    }

    @Test
    void deve_premiar_todos_os_participantes_sem_duplicar_xp() {
        // Isola clubes dos outros critérios e força a primeira participação ativa de todos.
        jdbc.update("UPDATE conquista SET meta_valor = 999999 WHERE criterio <> 'clubes'");
        jdbc.update(
                "DELETE FROM conquista_usuario WHERE conquista_id IN (SELECT id FROM conquista WHERE criterio = 'clubes')");
        jdbc.update("UPDATE clube_do_livro SET excluido_em = now()");
        int xpCriador = xp(CRIADOR);
        int xpMembro = xp(MEMBRO);
        criar("Primeiro", null, List.of(MEMBRO));
        assertThat(xp(CRIADOR)).isEqualTo(xpCriador + 75);
        assertThat(xp(MEMBRO)).isEqualTo(xpMembro + 75);
        criar("Segundo", null, List.of(MEMBRO));
        assertThat(xp(CRIADOR)).isEqualTo(xpCriador + 75);
        assertThat(xp(MEMBRO)).isEqualTo(xpMembro + 75);
        for (UUID id : List.of(CRIADOR, MEMBRO)) {
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM conquista_usuario cu JOIN conquista c ON c.id = cu.conquista_id WHERE cu.usuario_id = ? AND c.criterio = 'clubes'",
                                    Integer.class,
                                    id))
                    .isEqualTo(1);
        }
    }

    @Test
    void nao_deve_contar_clubes_excluidos_na_conquista() {
        jdbc.update(
                "DELETE FROM conquista_usuario WHERE usuario_id = ? AND conquista_id IN (SELECT id FROM conquista WHERE criterio = 'clubes')",
                CRIADOR);
        jdbc.update("UPDATE clube_do_livro SET excluido_em = now()");
        jdbc.update("UPDATE conquista SET meta_valor = 2 WHERE criterio = 'clubes'");
        criar("Único ativo", null, List.of());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM conquista_usuario cu JOIN conquista c ON c.id = cu.conquista_id WHERE cu.usuario_id = ? AND c.criterio = 'clubes'",
                                Integer.class,
                                CRIADOR))
                .isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deve_reverter_clube_membros_conquistas_e_xp_se_recompensas_falharem() {
        int xpCriador = xp(CRIADOR);
        int xpMembro = xp(MEMBRO);
        int conquistas = total("conquista_usuario");
        doAnswer(
                        invocacao -> {
                            invocacao.callRealMethod();
                            throw new IllegalStateException(
                                    "Falha simulada após avaliar as recompensas");
                        })
                .when(conquistaService)
                .avaliar(MEMBRO);
        erro(
                new CriarClubeRequest("Rollback", null, LIVRO, List.of(MEMBRO)),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR");
        assertThat(xp(CRIADOR)).isEqualTo(xpCriador);
        assertThat(xp(MEMBRO)).isEqualTo(xpMembro);
        assertThat(total("conquista_usuario")).isEqualTo(conquistas);
    }

    private UUID criar(String nome, String descricao, List<UUID> membros) {
        var resultado =
                mvc.post()
                        .uri("/api/clubes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                mapper.writeValueAsString(
                                        new CriarClubeRequest(nome, descricao, LIVRO, membros)))
                        .exchange();
        assertThat(resultado)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsOnlyKeys("id", "nome", "livro");
        assertThat(resultado).bodyJson().extractingPath("$.nome").isEqualTo(nome);
        assertThat(resultado)
                .bodyJson()
                .extractingPath("$.livro")
                .asMap()
                .containsOnlyKeys("titulo", "capaUrl")
                .containsEntry("titulo", "Dom Casmurro")
                .containsEntry("capaUrl", capa());
        entityManager.flush();
        return UUID.fromString(
                mapper.readTree(resultado.getResponse().getContentAsByteArray())
                        .get("id")
                        .asString());
    }

    private void erro(CriarClubeRequest request, HttpStatus status, String codigo) {
        int clubes = total("clube_do_livro");
        int membros = total("membro_clube");
        assertThat(
                        mvc.post()
                                .uri("/api/clubes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request)))
                .hasStatus(status)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsOnlyKeys("code", "message")
                .containsEntry("code", codigo);
        assertThat(total("clube_do_livro")).isEqualTo(clubes);
        assertThat(total("membro_clube")).isEqualTo(membros);
    }

    private int total(String tabela) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabela, Integer.class);
    }

    private int xp(UUID id) {
        return jdbc.queryForObject("SELECT xp_total FROM usuario WHERE id = ?", Integer.class, id);
    }

    private String capa() {
        return jdbc.queryForObject("SELECT capa_url FROM livro WHERE id = ?", String.class, LIVRO);
    }
}
