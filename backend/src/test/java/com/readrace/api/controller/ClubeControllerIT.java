package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import jakarta.persistence.EntityManager;

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

import com.readrace.api.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GET /api/clubes/{clubeId}")
class ClubeControllerIT {
    // Ids do seed (V4). O clube 1 tem 8 membros e lê Dom Casmurro; o usuário fixo é o 1º.
    private static final String CLUBE = "50000000-0000-0000-0000-000000000001";
    private static final String COMUNIDADE = "60000000-0000-0000-0000-000000000001";
    private static final String DOM_CASMURRO = "30000000-0000-0000-0000-000000000001";
    private static final String USUARIO_1 = "00000000-0000-0000-0000-000000000001";
    private static final String USUARIO_2 = "00000000-0000-0000-0000-000000000002";
    private static final String CLUBE_2 = "50000000-0000-0000-0000-000000000002";
    private static final String CLUBE_3 = "50000000-0000-0000-0000-000000000003";
    private static final String CLUBE_4 = "50000000-0000-0000-0000-000000000004";
    private static final String URL = "/api/clubes/" + CLUBE;

    @Autowired private MockMvcTester mvc;

    @Autowired private JdbcTemplate jdbc;

    @Autowired private EntityManager entityManager;

    @Test
    void deve_devolver_o_nome_do_clube_no_cabecalho() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.nome")
                .isEqualTo("Clube dos Clássicos Brasileiros");
    }

    @Test
    @DisplayName("livroAtual traz o id, porque o modal de registrar leitura precisa dele")
    void deve_devolver_o_livro_atual_com_id_titulo_e_autor() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.livroAtual.id")
                .isEqualTo(DOM_CASMURRO);
        assertThat(mvc.get().uri(URL))
                .bodyJson()
                .extractingPath("$.livroAtual.titulo")
                .isEqualTo("Dom Casmurro");
        assertThat(mvc.get().uri(URL))
                .bodyJson()
                .extractingPath("$.livroAtual.autor")
                .isEqualTo("Machado de Assis");
    }

    @Test
    @DisplayName("o clube tem 8 membros no seed, mas a tela mostra 7")
    void deve_cortar_o_ranking_em_sete_posicoes() {
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
                .isEqualTo(7);
    }

    @Test
    void deve_numerar_as_posicoes_de_um_a_sete() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.ranking[*].posicao")
                .asList()
                .containsExactly(1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    @DisplayName("pontos do seed: 1200 - posicao*83 - 17; o 8º membro (519) fica de fora")
    void deve_ordenar_do_maior_para_o_menor_numero_de_pontos() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.ranking[*].pontos")
                .asList()
                .containsExactly(1100, 1017, 934, 851, 768, 685, 602);
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
    @DisplayName("o ranking é em Pontos: nenhum campo de XP entra na resposta")
    void nao_deve_expor_xp() {
        assertThat(corpo(URL)).doesNotContainIgnoringCase("xp");
    }

    @Test
    @DisplayName("meuCargo: administrador quando o usuário atual lidera o clube")
    void deve_devolver_administrador_quando_o_usuario_atual_e_lider_do_clube() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.meuCargo")
                .isEqualTo("administrador");
    }

    @Test
    @DisplayName(
            "meuCargo: membro quando o usuário atual é membro comum (V13 colocou-o no clube 3)")
    void deve_devolver_membro_quando_o_usuario_atual_e_membro_comum() {
        assertThat(mvc.get().uri("/api/clubes/{id}", CLUBE_3))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.meuCargo")
                .isEqualTo("membro");
    }

    @Test
    @DisplayName("meuCargo: null quando o usuário atual não participa do clube")
    void deve_devolver_null_quando_o_usuario_atual_nao_participa_do_clube() {
        assertThat(mvc.get().uri("/api/clubes/{id}", CLUBE_4))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.meuCargo")
                .isNull();
    }

    @Test
    @DisplayName("temQuizAtivo: true quando o clube já tem quiz publicado (V13 criou o do clube 3)")
    void deve_indicar_quiz_ativo_quando_o_clube_tem_quiz() {
        assertThat(mvc.get().uri("/api/clubes/{id}", CLUBE_3))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.temQuizAtivo")
                .isEqualTo(true);
    }

    @Test
    @DisplayName("temQuizAtivo: false quando o clube ainda não tem quiz")
    void deve_indicar_sem_quiz_ativo_quando_o_clube_nao_tem_quiz() {
        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.temQuizAtivo")
                .isEqualTo(false);
    }

    @Test
    @DisplayName("clube 3 (V17): o usuário de demonstração começa em 5º, com 790, logo atrás do 4º")
    void deve_colocar_o_usuario_de_demonstracao_em_quinto_no_clube_3() {
        assertThat(mvc.get().uri("/api/clubes/{id}", CLUBE_3))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.ranking[*].pontos")
                .asList()
                .containsExactly(1066, 983, 900, 817, 790, 734, 651);
        assertThat(mvc.get().uri("/api/clubes/{id}", CLUBE_3))
                .bodyJson()
                .extractingPath("$.ranking[4].usuario.id")
                .isEqualTo(USUARIO_1);
    }

    @Test
    @DisplayName("clube 3 (V17): continua com os mesmos 9 membros e um único quiz ativo")
    void deve_manter_os_membros_e_o_quiz_do_clube_3() {
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM membro_clube WHERE clube_id = ?::uuid",
                                Integer.class,
                                CLUBE_3))
                .isEqualTo(9);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM quiz WHERE clube_id = ?::uuid AND ativo",
                                Integer.class,
                                CLUBE_3))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("clube 2 (V17 não mexe): o usuário de demonstração segue em 1º")
    void deve_manter_o_usuario_de_demonstracao_em_primeiro_no_clube_2() {
        assertThat(mvc.get().uri("/api/clubes/{id}", CLUBE_2))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.ranking[0].usuario.id")
                .isEqualTo(USUARIO_1);
    }

    @Test
    @Transactional
    void lider_unico_sai_e_promove_membro_com_mais_pontos() {
        UUID clubeId = UUID.fromString(CLUBE);
        UUID liderId = UUID.fromString(USUARIO_1);
        UUID sucessorId =
                jdbc.queryForObject(
                        "SELECT usuario_id FROM membro_clube WHERE clube_id = ? AND usuario_id <> ?"
                                + " ORDER BY pontos DESC, entrou_em, usuario_id LIMIT 1",
                        UUID.class,
                        clubeId,
                        liderId);
        String sucessorNome =
                jdbc.queryForObject(
                        "SELECT nome FROM usuario WHERE id = ?", String.class, sucessorId);

        assertThat(mvc.get().uri("/api/clubes/{id}", CLUBE))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.proximoLider.id")
                .isEqualTo(sucessorId.toString());

        assertThat(mvc.delete().uri("/api/clubes/{id}/membros/eu", CLUBE))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.novoLider.nome")
                .isEqualTo(sucessorNome);
        entityManager.flush();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT cargo_clube::text FROM membro_clube"
                                        + " WHERE clube_id = ? AND usuario_id = ?",
                                String.class,
                                clubeId,
                                sucessorId))
                .isEqualTo("administrador");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM membro_clube"
                                        + " WHERE clube_id = ? AND cargo_clube = 'administrador'",
                                Integer.class,
                                clubeId))
                .isEqualTo(1);
    }

    @Test
    @Transactional
    void empate_em_pontos_promove_quem_entrou_primeiro() {
        UUID clubeId = UUID.fromString(CLUBE);
        UUID liderId = UUID.fromString(USUARIO_1);
        UUID maisAntigo = UUID.fromString(USUARIO_2);
        UUID maisRecente = UUID.fromString("00000000-0000-0000-0000-000000000003");
        jdbc.update(
                "UPDATE membro_clube SET pontos = 2000, entrou_em = '2020-01-01T00:00:00Z'"
                        + " WHERE clube_id = ? AND usuario_id = ?",
                clubeId,
                maisAntigo);
        jdbc.update(
                "UPDATE membro_clube SET pontos = 2000, entrou_em = '2021-01-01T00:00:00Z'"
                        + " WHERE clube_id = ? AND usuario_id = ?",
                clubeId,
                maisRecente);

        assertThat(mvc.delete().uri("/api/clubes/{id}/membros/eu", CLUBE))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.novoLider.id")
                .isEqualTo(maisAntigo.toString());
    }

    @Test
    @Transactional
    void membro_comum_sai_sem_alterar_lideranca() {
        UUID clubeId = UUID.fromString(CLUBE_3);
        UUID liderId = UUID.fromString(USUARIO_2);

        assertThat(mvc.delete().uri("/api/clubes/{id}/membros/eu", CLUBE_3))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.novoLider")
                .isNull();
        entityManager.flush();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT cargo_clube::text FROM membro_clube"
                                        + " WHERE clube_id = ? AND usuario_id = ?",
                                String.class,
                                clubeId,
                                liderId))
                .isEqualTo("administrador");
    }

    @Test
    @Transactional
    void deve_concatenar_os_autores_na_ordem_do_vinculo() {
        jdbc.update(
                "INSERT INTO autor (id, nome) VALUES (?, 'Autora de teste')",
                UUID.fromString("19000000-0000-0000-0000-000000000001"));
        jdbc.update(
                "INSERT INTO livro_autor (livro_id, autor_id, ordem) VALUES (?, ?, 2)",
                UUID.fromString(DOM_CASMURRO),
                UUID.fromString("19000000-0000-0000-0000-000000000001"));

        assertThat(mvc.get().uri(URL))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.livroAtual.autor")
                .isEqualTo("Machado de Assis, Autora de teste");
    }

    @Test
    @Transactional
    @DisplayName("empate em Pontos não deixa a ordem ao acaso: desempata pelo nome")
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
    @DisplayName("clube sem membros devolve ranking vazio, não erro")
    void deve_devolver_ranking_vazio_quando_o_clube_nao_tem_membros() {
        UUID clubeVazio = UUID.fromString("59000000-0000-0000-0000-000000000001");
        jdbc.update(
                "INSERT INTO clube_do_livro (id, livro_id, nome) VALUES (?, ?, 'Clube sem membros')",
                clubeVazio,
                UUID.fromString(DOM_CASMURRO));

        assertThat(mvc.get().uri("/api/clubes/{id}", clubeVazio))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.ranking.length()")
                .isEqualTo(0);
    }

    @Test
    void deve_devolver_404_no_formato_padrao_quando_o_clube_nao_existir() {
        assertThat(mvc.get().uri("/api/clubes/{id}", UUID.randomUUID()))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(mvc.get().uri("/api/clubes/{id}", UUID.randomUUID()))
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("Clube não encontrado.");
    }

    @Test
    @DisplayName("id de comunidade responde 404: comunidade não tem livro atual nem ranking")
    void deve_devolver_404_para_id_de_comunidade() {
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM comunidade WHERE id = ?",
                                Integer.class,
                                UUID.fromString(COMUNIDADE)))
                .isEqualTo(1);
        assertThat(mvc.get().uri("/api/clubes/{id}", COMUNIDADE))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @Transactional
    void deve_devolver_404_quando_o_clube_estiver_excluido() {
        jdbc.update(
                "UPDATE clube_do_livro SET excluido_em = now() WHERE id = ?",
                UUID.fromString(CLUBE));

        assertThat(mvc.get().uri(URL)).hasStatus(HttpStatus.NOT_FOUND);
    }

    private String corpo(String url) {
        try {
            return mvc.get().uri(url).exchange().getResponse().getContentAsString();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
