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
    @DisplayName("empate em Pontos desempata pelo nome, como na Página do clube")
    void deve_desempatar_por_nome() {
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
