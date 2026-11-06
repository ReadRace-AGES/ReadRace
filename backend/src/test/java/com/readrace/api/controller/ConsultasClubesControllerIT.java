package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
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
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.model.UsuarioId;
import com.readrace.api.service.UsuarioAtual;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ConsultasClubesControllerIT {
    private static final UUID CLUBE = clube(1);
    private static final String URL = "/api/clubes/" + CLUBE;
    @Autowired private MockMvcTester mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory emf;
    @MockitoSpyBean private UsuarioAtual usuarioAtual;

    @ParameterizedTest
    @CsvSource({"1,membro", "2,membro", "9,pendente", "10,pendente"})
    void lista_e_detalhe_refletem_usuario_atual(int usuario, String situacao) throws Exception {
        como(usuario);
        Map<String, Object> linha = linha(CLUBE);
        assertThat(linha)
                .containsOnlyKeys("id", "nome", "capaUrl", "livro", "membros", "situacao")
                .containsEntry("situacao", situacao)
                .containsEntry("membros", 8)
                .containsEntry("nome", "Clube dos Clássicos Brasileiros");
        assertThat((Map<?, ?>) linha.get("livro"))
                .isEqualTo(Map.of("titulo", "Dom Casmurro", "autor", "Machado de Assis"));
        assertThat(linha.get("capaUrl"))
                .isEqualTo(
                        jdbc.queryForObject(
                                "SELECT capa_url FROM clube_do_livro WHERE id = ?",
                                String.class,
                                CLUBE));
        Map<String, Object> detalhe = json(URL);
        assertThat(detalhe)
                .containsEntry("situacao", situacao)
                .containsKeys(
                        "id",
                        "nome",
                        "livroAtual",
                        "ranking",
                        "meuCargo",
                        "temQuizAtivo",
                        "proximoLider");
        if (usuario == 1) {
            assertThat(detalhe)
                    .containsEntry("meuCargo", "administrador")
                    .containsEntry("solicitacoesPendentes", 2);
        } else {
            assertThat(detalhe).doesNotContainKey("solicitacoesPendentes");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"aceita", "recusada"})
    void pedido_respondido_nao_define_situacao_pendente(String situacao) throws Exception {
        responderSeed(situacao);
        como(9);
        assertThat(linha(CLUBE)).containsEntry("situacao", "nenhuma");
        assertThat(json(URL))
                .containsEntry("situacao", "nenhuma")
                .containsEntry("meuCargo", null)
                .doesNotContainKey("solicitacoesPendentes");
    }

    @Test
    void associacao_prevalece_sobre_pendente_e_contagem_usa_apenas_vinculos() throws Exception {
        jdbc.update(
                "INSERT INTO membro_clube (id, clube_id, usuario_id, cargo_clube) VALUES (?, ?, ?, 'membro')",
                UUID.randomUUID(),
                CLUBE,
                usuario(9));
        como(9);
        assertThat(linha(CLUBE)).containsEntry("situacao", "membro").containsEntry("membros", 9);
        assertThat(json(URL))
                .containsEntry("situacao", "membro")
                .doesNotContainKey("solicitacoesPendentes");
    }

    @Test
    void visitante_abre_detalhe_e_forum_mas_nao_ranking_ou_solicitacoes() throws Exception {
        como(1);
        String url = "/api/clubes/" + clube(4);
        assertThat(linha(clube(4))).containsEntry("situacao", "nenhuma");
        assertThat(json(url))
                .containsEntry("situacao", "nenhuma")
                .containsEntry("meuCargo", null)
                .doesNotContainKey("solicitacoesPendentes");
        erro(url + "/ranking", HttpStatus.FORBIDDEN, "SO_MEMBRO_VE_RANKING");
        assertThat(json(url + "/posts")).containsEntry("podeEscrever", false);
        erro(url + "/solicitacoes", HttpStatus.FORBIDDEN, "SO_LIDER_CRIA_QUIZ");
    }

    @Test
    void lista_ordena_por_nome_id_exclui_inativos_e_nao_faz_n_mais_um() throws Exception {
        jdbc.update("UPDATE clube_do_livro SET nome = 'Mesmo nome'");
        jdbc.update("UPDATE clube_do_livro SET excluido_em = now() WHERE id = ?", clube(5));
        em.clear();
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        boolean habilitada = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        stats.clear();
        try {
            assertThat(JsonPath.<List<String>>read(corpo("/api/clubes"), "$.clubes[*].id"))
                    .containsExactly(
                            CLUBE.toString(),
                            clube(2).toString(),
                            clube(3).toString(),
                            clube(4).toString());
            assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        } finally {
            stats.setStatisticsEnabled(habilitada);
        }
        jdbc.update("UPDATE clube_do_livro SET nome = 'A primeiro' WHERE id = ?", clube(4));
        assertThat(JsonPath.<List<String>>read(corpo("/api/clubes"), "$.clubes[*].id"))
                .startsWith(clube(4).toString());
    }

    @Test
    void lista_vazia_e_clube_sem_membros() throws Exception {
        jdbc.update("DELETE FROM membro_clube WHERE clube_id = ?", clube(4));
        assertThat(linha(clube(4))).containsEntry("membros", 0);
        jdbc.update("UPDATE clube_do_livro SET excluido_em = now()");
        assertThat(json("/api/clubes")).isEqualTo(Map.of("clubes", List.of()));
    }

    @Test
    void autores_respeitam_ordem_sem_multiplicar_membros_e_aceitam_ausencia() throws Exception {
        UUID livro = UUID.fromString("30000000-0000-0000-0000-000000000001");
        UUID autor = UUID.randomUUID();
        jdbc.update("INSERT INTO autor (id, nome) VALUES (?, 'Segundo autor')", autor);
        jdbc.update(
                "INSERT INTO livro_autor (livro_id, autor_id, ordem) VALUES (?, ?, 2)",
                livro,
                autor);
        assertThat((Map<?, ?>) linha(CLUBE).get("livro"))
                .isEqualTo(
                        Map.of(
                                "titulo",
                                "Dom Casmurro",
                                "autor",
                                "Machado de Assis, Segundo autor"));
        assertThat(linha(CLUBE)).containsEntry("membros", 8);
        jdbc.update("DELETE FROM livro_autor WHERE livro_id = ?", livro);
        assertThat((Map<?, ?>) linha(CLUBE).get("livro")).containsValue(null);
    }

    @Test
    void lista_administrativa_ordena_e_exclui_respondidos_e_outros_clubes() throws Exception {
        como(1);
        jdbc.update(
                "UPDATE solicitacao_entrada_clube SET criada_em = '2026-01-01T00:00:00Z' WHERE clube_id = ?",
                CLUBE);
        List<Map<String, Object>> pedidos =
                JsonPath.read(corpo(URL + "/solicitacoes"), "$.solicitacoes");
        assertThat(pedidos).hasSize(2);
        assertThat(pedidos)
                .extracting(p -> p.get("id"))
                .containsExactly(
                        "78000000-0000-0000-0000-000000000001",
                        "78000000-0000-0000-0000-000000000002");
        assertThat(pedidos.getFirst()).containsOnlyKeys("id", "usuario", "criadaEm");
        assertThat((Map<String, Object>) pedidos.getFirst().get("usuario"))
                .containsOnlyKeys("id", "nome", "nomeUsuario", "avatarUrl")
                .containsEntry("id", usuario(9).toString())
                .containsEntry("nome", "Pedro Almeida")
                .containsEntry("nomeUsuario", "pedroalmeida");
        jdbc.update(
                "UPDATE solicitacao_entrada_clube SET criada_em = '2025-01-01T00:00:00Z' WHERE clube_id = ? AND usuario_id = ?",
                CLUBE,
                usuario(10));
        assertThat(
                        JsonPath.<List<String>>read(
                                corpo(URL + "/solicitacoes"), "$.solicitacoes[*].usuario.id"))
                .containsExactly(usuario(10).toString(), usuario(9).toString());
        responderSeed("recusada");
        assertThat(
                        JsonPath.<List<String>>read(
                                corpo(URL + "/solicitacoes"), "$.solicitacoes[*].usuario.id"))
                .containsExactly(usuario(10).toString());
        assertThat(json(URL)).containsEntry("solicitacoesPendentes", 1);
        jdbc.update(
                "UPDATE solicitacao_entrada_clube SET situacao = 'aceita', respondida_em = now(), administrador_id = ? WHERE clube_id = ?",
                usuario(1),
                CLUBE);
        assertThat(json(URL + "/solicitacoes")).isEqualTo(Map.of("solicitacoes", List.of()));
        assertThat(json(URL)).containsEntry("solicitacoesPendentes", 0);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 9})
    void membro_e_pendente_nao_administram(int usuario) {
        como(usuario);
        assertThat(mvc.get().uri(URL + "/solicitacoes"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("code", "SO_LIDER_CRIA_QUIZ")
                .containsEntry("message", "Só o administrador do clube pode ver as solicitações.");
    }

    @Test
    void qualquer_administrador_atual_pode_listar_e_sucessao_preserva_acesso() throws Exception {
        jdbc.update(
                "UPDATE membro_clube SET cargo_clube = 'administrador' WHERE clube_id = ? AND usuario_id = ?",
                CLUBE,
                usuario(2));
        como(2);
        assertThat(json(URL)).containsEntry("solicitacoesPendentes", 2);
        assertThat(json(URL + "/solicitacoes")).containsKey("solicitacoes");
        // Retorna ao estado do seed para exercer a transferência real integrada pela #237.
        jdbc.update(
                "UPDATE membro_clube SET cargo_clube = 'membro' WHERE clube_id = ? AND usuario_id = ?",
                CLUBE,
                usuario(2));
        em.clear();
        como(1);
        assertThat(mvc.delete().uri(URL + "/membros/eu")).hasStatusOk();
        como(2);
        assertThat(json(URL))
                .containsEntry("meuCargo", "administrador")
                .containsEntry("solicitacoesPendentes", 2);
        assertThat(json(URL + "/solicitacoes")).containsKey("solicitacoes");
        como(1);
        erro(URL + "/solicitacoes", HttpStatus.FORBIDDEN, "SO_LIDER_CRIA_QUIZ");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void clube_ausente_ou_excluido_preserva_codigos(boolean excluido) {
        UUID id = excluido ? CLUBE : UUID.randomUUID();
        if (excluido) jdbc.update("UPDATE clube_do_livro SET excluido_em = now() WHERE id = ?", id);
        erro("/api/clubes/" + id, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
        erro("/api/clubes/" + id + "/solicitacoes", HttpStatus.NOT_FOUND, "CLUBE_NAO_ENCONTRADO");
    }

    private void responderSeed(String situacao) {
        jdbc.update(
                "UPDATE solicitacao_entrada_clube SET situacao = ?::situacao_solicitacao_clube, respondida_em = now(), administrador_id = ? WHERE clube_id = ? AND usuario_id = ?",
                situacao,
                usuario(1),
                CLUBE,
                usuario(9));
    }

    private void como(int numero) {
        doReturn(new UsuarioId(usuario(numero))).when(usuarioAtual).idDoUsuarioAtual();
    }

    private Map<String, Object> linha(UUID id) throws Exception {
        List<Map<String, Object>> clubes = JsonPath.read(corpo("/api/clubes"), "$.clubes");
        return clubes.stream()
                .filter(c -> c.get("id").equals(id.toString()))
                .findFirst()
                .orElseThrow();
    }

    private Map<String, Object> json(String rota) throws Exception {
        return JsonPath.read(corpo(rota), "$");
    }

    private String corpo(String rota) throws Exception {
        var resposta = mvc.get().uri(rota).exchange();
        assertThat(resposta).hasStatusOk();
        return resposta.getResponse().getContentAsString();
    }

    private void erro(String rota, HttpStatus status, String codigo) {
        assertThat(mvc.get().uri(rota))
                .hasStatus(status)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo(codigo);
    }

    private static UUID clube(int numero) {
        return UUID.fromString("50000000-0000-0000-0000-%012d".formatted(numero));
    }

    private static UUID usuario(int numero) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(numero));
    }
}
