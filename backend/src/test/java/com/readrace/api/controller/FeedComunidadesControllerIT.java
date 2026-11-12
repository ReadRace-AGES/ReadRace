package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.readrace.api.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class FeedComunidadesControllerIT {
    private static final UUID EU = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String COMUNIDADE = "60000000-0000-0000-0000-00000000000";
    @Autowired private MockMvcTester mvc;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void seed_preserva_contrato_antigo_e_acrescenta_sugestoes_com_contagens() throws Exception {
        Map<String, Object> feed = feed();
        assertThat(feed)
                .containsOnlyKeys("usuario", "clubes", "comunidades", "comunidadesSugeridas");
        Map<String, Object> usuario = JsonPath.read(feed, "$.usuario");
        assertThat(usuario)
                .containsOnlyKeys("nome", "sequenciaDias", "leuHoje", "diasAtePerder")
                .containsEntry("nome", "Daniel Ribeiro");
        List<Map<String, Object>> clubes = JsonPath.read(feed, "$.clubes");
        assertThat(clubes).hasSize(3);
        for (var clube : clubes) {
            assertThat(clube)
                    .containsOnlyKeys("id", "nome", "capaUrl", "livroAtual", "totalMembros");
            Map<String, Object> livro = JsonPath.read(clube, "$.livroAtual");
            assertThat(livro).containsOnlyKeys("titulo", "autor");
            assertThat(((Number) clube.get("totalMembros")).longValue())
                    .isEqualTo(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM membro_clube WHERE clube_id = ?::uuid",
                                    Long.class,
                                    clube.get("id")));
        }
        assertThat(JsonPath.<List<String>>read(feed, "$.comunidades[*].id"))
                .containsExactlyInAnyOrder(COMUNIDADE + "1", COMUNIDADE + "2", COMUNIDADE + "3");
        List<Map<String, Object>> minhas = JsonPath.read(feed, "$.comunidades");
        for (var comunidade : minhas) {
            assertThat(comunidade).containsOnlyKeys("id", "nome", "capaUrl", "totalMembros");
            assertThat(((Number) comunidade.get("totalMembros")).longValue()).isEqualTo(10L);
            assertThat(comunidade.get("capaUrl")).isEqualTo(imagem(comunidade.get("id")));
        }
        assertThat(JsonPath.<List<String>>read(feed, "$.comunidadesSugeridas[*].id"))
                .containsExactly(COMUNIDADE + "4", COMUNIDADE + "5");
        List<Map<String, Object>> sugestoes = JsonPath.read(feed, "$.comunidadesSugeridas");
        for (var comunidade : sugestoes) {
            assertThat(comunidade).containsOnlyKeys("id", "nome", "imagemUrl", "membros");
            assertThat(((Number) comunidade.get("membros")).longValue()).isEqualTo(9L);
            assertThat(comunidade.get("imagemUrl")).isEqualTo(imagem(comunidade.get("id")));
        }
    }

    @Test
    void exclui_participacoes_e_excluidas_limita_cinco_e_desempata_nome_por_id() throws Exception {
        participarDeTodas();
        // Inserção invertida: a ordem deve vir de nome e ID, não da ordem física das linhas.
        for (int i = 7; i >= 1; i--) {
            criar(sugestao(i), i <= 2 ? "A sugestão" : "B sugestão " + i);
        }
        UUID excluida = UUID.randomUUID();
        criar(excluida, "A excluída");
        jdbc.update("UPDATE comunidade SET excluido_em = now() WHERE id = ?", excluida);
        UUID participante = UUID.randomUUID();
        criar(participante, "A participante");
        vincular(participante, EU);
        vincular(sugestao(1), UUID.fromString("00000000-0000-0000-0000-000000000002"));

        var resposta = feed();
        List<String> ids = JsonPath.read(resposta, "$.comunidadesSugeridas[*].id");
        assertThat(ids)
                .containsExactly(
                        sugestao(1).toString(),
                        sugestao(2).toString(),
                        sugestao(3).toString(),
                        sugestao(4).toString(),
                        sugestao(5).toString());
        assertThat(JsonPath.<Integer>read(resposta, "$.comunidadesSugeridas[0].membros"))
                .isEqualTo(1);
        assertThat(JsonPath.<Integer>read(resposta, "$.comunidadesSugeridas[1].membros")).isZero();
        assertThat(JsonPath.<List<String>>read(feed(), "$.comunidadesSugeridas[*].id"))
                .isEqualTo(ids);
    }

    @Test
    void retorna_array_vazio_quando_todas_as_comunidades_ativas_tem_participacao()
            throws Exception {
        participarDeTodas();
        assertThat(JsonPath.<List<?>>read(feed(), "$.comunidadesSugeridas")).isEmpty();
    }

    @Test
    void participar_move_para_minhas_atualiza_contagem_e_continua_idempotente() throws Exception {
        var antes = feed();
        String id = COMUNIDADE + "4";
        for (int i = 0; i < 2; i++) {
            assertThat(mvc.post().uri("/api/comunidades/" + id + "/membros/eu"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$")
                    .asMap()
                    .containsEntry("sigo", true)
                    .containsEntry("seguidores", 10);
            var depois = feed();
            assertThat(JsonPath.<List<String>>read(depois, "$.comunidadesSugeridas[*].id"))
                    .containsExactly(COMUNIDADE + "5");
            List<Map<String, Object>> minhas = JsonPath.read(depois, "$.comunidades");
            assertThat(minhas).hasSize(4);
            assertThat(minhas.stream().filter(c -> id.equals(c.get("id"))).toList())
                    .singleElement()
                    .satisfies(c -> assertThat(c).containsEntry("totalMembros", 10));
            assertThat(depois.get("usuario")).isEqualTo(antes.get("usuario"));
            assertThat(depois.get("clubes")).isEqualTo(antes.get("clubes"));
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM membro_comunidade WHERE comunidade_id = ?::uuid AND usuario_id = ?",
                                Long.class,
                                id,
                                EU))
                .isEqualTo(1L);
    }

    private Map<String, Object> feed() throws Exception {
        var resultado = mvc.get().uri("/api/feed/comunidades").exchange();
        assertThat(resultado).hasStatusOk();
        return JsonPath.read(resultado.getResponse().getContentAsString(), "$");
    }

    private String imagem(Object id) {
        return jdbc.queryForObject(
                "SELECT imagem_url FROM comunidade WHERE id = ?::uuid", String.class, id);
    }

    private void participarDeTodas() {
        jdbc.update(
                "INSERT INTO membro_comunidade (id, comunidade_id, usuario_id, cargo_comunidade) SELECT gen_random_uuid(), id, ?, 'membro' FROM comunidade WHERE excluido_em IS NULL ON CONFLICT (comunidade_id, usuario_id) DO NOTHING",
                EU);
    }

    private void criar(UUID id, String nome) {
        jdbc.update("INSERT INTO comunidade (id, nome) VALUES (?, ?)", id, nome);
    }

    private void vincular(UUID comunidade, UUID usuario) {
        jdbc.update(
                "INSERT INTO membro_comunidade (id, comunidade_id, usuario_id, cargo_comunidade) VALUES (?, ?, ?, 'membro')",
                UUID.randomUUID(),
                comunidade,
                usuario);
    }

    private UUID sugestao(int numero) {
        return UUID.fromString("61000000-0000-0000-0000-00000000000" + numero);
    }
}
