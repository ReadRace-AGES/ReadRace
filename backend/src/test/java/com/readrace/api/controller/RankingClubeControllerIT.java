package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.readrace.api.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GET /api/clubes/{clubeId}/ranking")
class RankingClubeControllerIT {
    private static final String CLUBE = "50000000-0000-0000-0000-000000000001";
    private static final String COMUNIDADE = "60000000-0000-0000-0000-000000000001";
    private static final String USUARIO_1 = "00000000-0000-0000-0000-000000000001";
    private static final String USUARIO_2 = "00000000-0000-0000-0000-000000000002";
    private static final String URL = "/api/clubes/" + CLUBE + "/ranking";
    private static final String URL_DA_PAGINA = "/api/clubes/" + CLUBE;

    @Autowired private MockMvcTester mvc;

    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("devolve id e nome do clube para o cabeçalho")
    void deve_devolver_id_e_nome_do_clube() {
        String nome =
                jdbc.queryForObject(
                        "SELECT nome FROM clube_do_livro WHERE id = ?",
                        String.class,
                        UUID.fromString(CLUBE));

        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.clube.id")
                .isEqualTo(CLUBE);
        assertThat(mvc.get().uri(URL)).bodyJson().extractingPath("$.clube.nome").isEqualTo(nome);
    }

    @Test
    @DisplayName("o clube tem 8 membros no seed e o ranking completo mostra os 8")
    void deve_devolver_todos_os_membros() {
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM membro_clube WHERE clube_id = ?",
                                Integer.class,
                                UUID.fromString(CLUBE)))
                .isEqualTo(8);
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.ranking.length()")
                .isEqualTo(8);
    }

    @Test
    void deve_numerar_as_posicoes_de_um_a_oito() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.ranking[*].posicao")
                .asList()
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    @DisplayName("pontos do seed: 1200 - posicao*83 - 17; o 8º membro (519) aparece aqui")
    void deve_ordenar_do_maior_para_o_menor_numero_de_pontos() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.ranking[*].pontos")
                .asList()
                .containsExactly(1100, 1017, 934, 851, 768, 685, 602, 519);
    }

    @Test
    void deve_identificar_o_membro_de_cada_posicao() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.ranking[0].usuario.id")
                .isEqualTo(USUARIO_1);
        assertThat(mvc.get().uri(URL))
                .bodyJson()
                .extractingPath("$.ranking[0].usuario.nome")
                .isEqualTo("Daniel Ribeiro");
        assertThat(mvc.get().uri(URL))
                .bodyJson()
                .extractingPath("$.ranking[0].usuario.avatarUrl")
                .isEqualTo("https://i.pravatar.cc/300?img=12");
    }

    @Test
    @DisplayName("as 7 primeiras posições são as mesmas da Página do clube")
    void deve_repetir_as_sete_primeiras_posicoes_da_pagina_do_clube() {
        List<Object> daPagina = JsonPath.read(corpo(URL_DA_PAGINA), "$.ranking");
        List<Object> completo = JsonPath.read(corpo(URL), "$.ranking");

        assertThat(completo.subList(0, 7)).isEqualTo(daPagina);
    }

    @Test
    @Transactional
    @DisplayName("empate em Pontos mantém a mesma posição e ordena a apresentação pelo nome")
    void deve_ordenar_empatados_por_nome_sem_alterar_a_posicao() {
        jdbc.update(
                "UPDATE membro_clube SET pontos = 1100 WHERE clube_id = ? AND usuario_id = ?",
                UUID.fromString(CLUBE),
                UUID.fromString(USUARIO_2));

        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.ranking[*].usuario.nome")
                .asList()
                .startsWith("Ana Silva", "Daniel Ribeiro");
        for (String url : List.of(URL, URL_DA_PAGINA)) {
            assertThat(mvc.get().uri(url))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.ranking[*].posicao")
                    .asList()
                    .startsWith(1, 1, 3);
            // Daniel é o usuário autenticado: sua posição também é 1, embora venha em segundo.
            assertThat(mvc.get().uri(url))
                    .bodyJson()
                    .extractingPath("$.ranking[1].usuario.id")
                    .isEqualTo(USUARIO_1);
        }
    }

    @Test
    @Transactional
    void deve_manter_empate_intermediario_e_pular_as_posicoes_ocupadas() {
        jdbc.update(
                "UPDATE membro_clube SET pontos = 934 WHERE clube_id = ? AND usuario_id = ?",
                UUID.fromString(CLUBE),
                UUID.fromString(USUARIO_2));

        for (String url : List.of(URL, URL_DA_PAGINA)) {
            assertThat(mvc.get().uri(url))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.ranking[*].posicao")
                    .asList()
                    .startsWith(1, 2, 2, 4, 5, 6, 7);
        }
    }

    @Test
    @Transactional
    void deve_ordenar_por_uuid_quando_nome_e_pontos_sao_iguais() {
        jdbc.update(
                "UPDATE membro_clube SET pontos = 1100 WHERE clube_id = ? AND usuario_id = ?",
                UUID.fromString(CLUBE),
                UUID.fromString(USUARIO_2));
        jdbc.update(
                "UPDATE usuario SET nome = 'Daniel Ribeiro' WHERE id = ?",
                UUID.fromString(USUARIO_2));

        for (String url : List.of(URL, URL_DA_PAGINA)) {
            assertThat(mvc.get().uri(url))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.ranking[*].usuario.id")
                    .asList()
                    .startsWith(USUARIO_1, USUARIO_2);
            assertThat(mvc.get().uri(url))
                    .bodyJson()
                    .extractingPath("$.ranking[*].posicao")
                    .asList()
                    .startsWith(1, 1, 3);
        }
    }

    @Test
    @Transactional
    void nao_deve_contar_usuario_excluido_nem_participante_de_outro_clube() {
        jdbc.update(
                "UPDATE usuario SET excluido_em = NOW() WHERE id = ?", UUID.fromString(USUARIO_1));
        jdbc.update(
                "UPDATE membro_clube SET pontos = 9999 WHERE clube_id <> ?",
                UUID.fromString(CLUBE));

        assertThat(mvc.get().uri(URL_DA_PAGINA))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.ranking[*].posicao")
                .asList()
                .containsExactly(1, 2, 3, 4, 5, 6, 7);
        assertThat(mvc.get().uri(URL_DA_PAGINA))
                .bodyJson()
                .extractingPath("$.ranking[*].usuario.id")
                .asList()
                .doesNotContain(USUARIO_1);
    }

    @Test
    @Transactional
    void deve_calcular_empate_no_ranking_inteiro_antes_de_paginar_incluindo_o_usuario_atual() {
        UUID clubeId = UUID.fromString(CLUBE);
        jdbc.update("DELETE FROM membro_clube WHERE clube_id = ?", clubeId);
        for (int i = 0; i < 54; i++) {
            UUID usuarioId =
                    i == 50 ? UUID.fromString(USUARIO_1) : new UUID(0x2220000000000000L, i);
            String nome = "Participante %03d".formatted(i);
            if (i == 50) {
                jdbc.update("UPDATE usuario SET nome = ? WHERE id = ?", nome, usuarioId);
            } else {
                jdbc.update(
                        "INSERT INTO usuario (id, nome, nome_usuario, email) VALUES (?, ?, ?, ?)",
                        usuarioId,
                        nome,
                        "ranking222_" + i,
                        "ranking222_" + i + "@teste.com");
            }
            int pontos = i < 48 ? 1000 : i < 52 ? 500 : 100;
            jdbc.update(
                    "INSERT INTO membro_clube (id, clube_id, usuario_id, pontos, cargo_clube) VALUES (?, ?, ?, ?, 'membro')",
                    UUID.randomUUID(),
                    clubeId,
                    usuarioId,
                    pontos);
        }

        String primeira = corpo(URL);
        List<Integer> posicoes = JsonPath.read(primeira, "$.ranking[*].posicao");
        assertThat(posicoes).hasSize(50);
        assertThat(posicoes.subList(0, 48)).containsOnly(1);
        assertThat(posicoes.subList(48, 50)).containsExactly(49, 49);
        assertThat((String) JsonPath.read(primeira, "$.proximoCursor")).isEqualTo("1");

        String segunda = corpo(URL + "?cursor=1");
        assertThat(JsonPath.<List<Integer>>read(segunda, "$.ranking[*].posicao"))
                .containsExactly(49, 49, 53, 53);
        assertThat((String) JsonPath.read(segunda, "$.ranking[0].usuario.id")).isEqualTo(USUARIO_1);
        assertThat((Object) JsonPath.read(segunda, "$.proximoCursor")).isNull();
        List<String> idsPrimeira = JsonPath.read(primeira, "$.ranking[*].usuario.id");
        List<String> idsSegunda = JsonPath.read(segunda, "$.ranking[*].usuario.id");
        assertThat(idsPrimeira).doesNotContainAnyElementsOf(idsSegunda);
        assertThat(idsSegunda).doesNotHaveDuplicates();

        assertThat(mvc.get().uri(URL_DA_PAGINA))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.ranking[*].posicao")
                .asList()
                .containsExactly(1, 1, 1, 1, 1, 1, 1);
    }

    @Test
    @Transactional
    @DisplayName("pontos ganhos aparecem na próxima chamada")
    void deve_refletir_pontos_atualizados() {
        jdbc.update(
                "UPDATE membro_clube SET pontos = 5000 WHERE clube_id = ? AND usuario_id = ?",
                UUID.fromString(CLUBE),
                UUID.fromString(USUARIO_2));

        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.ranking[0].pontos")
                .isEqualTo(5000);
        assertThat(mvc.get().uri(URL))
                .bodyJson()
                .extractingPath("$.ranking[0].usuario.id")
                .isEqualTo(USUARIO_2);
    }

    @Test
    @DisplayName("o ranking é em Pontos: nenhum campo de XP entra na resposta")
    void nao_deve_expor_xp() {
        assertThat(corpo(URL)).doesNotContainIgnoringCase("xp");
    }

    @Test
    void deve_devolver_404_clube_nao_encontrado_quando_o_clube_nao_existir() {
        assertThat(mvc.get().uri("/api/clubes/{id}/ranking", UUID.randomUUID()))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("id de comunidade responde 404: comunidade não tem ranking")
    void deve_devolver_404_para_id_de_comunidade() {
        assertThat(mvc.get().uri("/api/clubes/{id}/ranking", COMUNIDADE))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    private String corpo(String url) {
        try {
            return mvc.get().uri(url).exchange().getResponse().getContentAsString();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
