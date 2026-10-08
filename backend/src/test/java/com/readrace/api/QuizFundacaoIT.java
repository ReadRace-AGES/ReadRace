package com.readrace.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@DisplayName("V13 - tabelas do quiz e seed do clube 3")
class QuizFundacaoIT {

    // Clube 3 = "Jornada pela Fantasia", livro atual = O Hobbit (seed V4).
    private static final UUID CLUBE_1 = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID CLUBE_2 = UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final UUID CLUBE_3 = UUID.fromString("50000000-0000-0000-0000-000000000003");
    private static final UUID O_HOBBIT = UUID.fromString("30000000-0000-0000-0000-000000000008");
    private static final UUID USUARIO_DEMO =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID QUIZ_SEED = UUID.fromString("71000000-0000-0000-0000-000000000001");

    @Autowired private JdbcTemplate jdbc;

    @Test
    void deve_ter_um_quiz_ativo_no_clube_3_com_tres_perguntas() {
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM quiz WHERE clube_id = ? AND ativo = true",
                                Integer.class,
                                CLUBE_3))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM pergunta_quiz WHERE quiz_id = ?",
                                Integer.class,
                                QUIZ_SEED))
                .isEqualTo(3);
    }

    @Test
    void cada_pergunta_do_seed_tem_quatro_alternativas_e_uma_correta() {
        assertThat(
                        jdbc.queryForList(
                                """
                                SELECT pergunta_id, count(*) AS total,
                                       count(*) FILTER (WHERE correta) AS corretas
                                FROM alternativa_quiz
                                WHERE pergunta_id IN
                                    (SELECT id FROM pergunta_quiz WHERE quiz_id = ?)
                                GROUP BY pergunta_id
                                """,
                                QUIZ_SEED))
                .hasSize(3)
                .allSatisfy(
                        linha -> {
                            assertThat(linha.get("total")).isEqualTo(4L);
                            assertThat(linha.get("corretas")).isEqualTo(1L);
                        });
    }

    @Test
    @DisplayName("usuário demo: administrador nos clubes 1 e 2, membro comum no clube 3")
    void deve_colocar_o_usuario_demo_como_membro_comum_de_um_terceiro_clube() {
        assertThat(cargoDe(CLUBE_1, USUARIO_DEMO)).isEqualTo("administrador");
        assertThat(cargoDe(CLUBE_2, USUARIO_DEMO)).isEqualTo("administrador");
        assertThat(cargoDe(CLUBE_3, USUARIO_DEMO)).isEqualTo("membro");
    }

    @Test
    @Transactional
    void deve_recusar_uma_segunda_resposta_do_mesmo_usuario_para_a_mesma_pergunta() {
        UUID perguntaId =
                jdbc.queryForObject(
                        "SELECT id FROM pergunta_quiz WHERE quiz_id = ? AND ordem = 1",
                        UUID.class,
                        QUIZ_SEED);
        UUID alternativaA =
                jdbc.queryForObject(
                        "SELECT id FROM alternativa_quiz WHERE pergunta_id = ? AND letra = 'A'",
                        UUID.class,
                        perguntaId);
        UUID alternativaB =
                jdbc.queryForObject(
                        "SELECT id FROM alternativa_quiz WHERE pergunta_id = ? AND letra = 'B'",
                        UUID.class,
                        perguntaId);

        jdbc.update(
                "INSERT INTO resposta_quiz (id, pergunta_id, usuario_id, alternativa_id, correta)"
                        + " VALUES (gen_random_uuid(), ?, ?, ?, true)",
                perguntaId,
                USUARIO_DEMO,
                alternativaA);

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO resposta_quiz (id, pergunta_id,"
                                                + " usuario_id, alternativa_id, correta) VALUES"
                                                + " (gen_random_uuid(), ?, ?, ?, false)",
                                        perguntaId,
                                        USUARIO_DEMO,
                                        alternativaB))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    @DisplayName("a unicidade é por pergunta, não por quiz: 1 usuário responde as 3 perguntas")
    void deve_permitir_o_mesmo_usuario_responder_as_tres_perguntas_do_mesmo_quiz() {
        List<UUID> perguntaIds =
                jdbc.queryForList(
                        "SELECT id FROM pergunta_quiz WHERE quiz_id = ? ORDER BY ordem",
                        UUID.class,
                        QUIZ_SEED);
        assertThat(perguntaIds).hasSize(3);

        for (UUID perguntaId : perguntaIds) {
            UUID alternativaId =
                    jdbc.queryForObject(
                            "SELECT id FROM alternativa_quiz WHERE pergunta_id = ? AND letra = 'A'",
                            UUID.class,
                            perguntaId);

            jdbc.update(
                    "INSERT INTO resposta_quiz (id, pergunta_id, usuario_id, alternativa_id,"
                            + " correta) VALUES (gen_random_uuid(), ?, ?, ?, false)",
                    perguntaId,
                    USUARIO_DEMO,
                    alternativaId);
        }

        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM resposta_quiz WHERE usuario_id = ?"
                                        + " AND pergunta_id IN (SELECT id FROM pergunta_quiz"
                                        + " WHERE quiz_id = ?)",
                                Integer.class,
                                USUARIO_DEMO,
                                QUIZ_SEED))
                .isEqualTo(3);
    }

    @Test
    @Transactional
    @DisplayName("resposta_quiz recusa uma alternativa que pertence a outra pergunta")
    void deve_recusar_resposta_com_alternativa_de_outra_pergunta() {
        UUID pergunta1 =
                jdbc.queryForObject(
                        "SELECT id FROM pergunta_quiz WHERE quiz_id = ? AND ordem = 1",
                        UUID.class,
                        QUIZ_SEED);
        UUID alternativaDaPergunta2 =
                jdbc.queryForObject(
                        "SELECT a.id FROM alternativa_quiz a"
                                + " JOIN pergunta_quiz p ON p.id = a.pergunta_id"
                                + " WHERE p.quiz_id = ? AND p.ordem = 2 AND a.letra = 'A'",
                        UUID.class,
                        QUIZ_SEED);

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO resposta_quiz (id, pergunta_id,"
                                                + " usuario_id, alternativa_id, correta) VALUES"
                                                + " (gen_random_uuid(), ?, ?, ?, false)",
                                        pergunta1,
                                        USUARIO_DEMO,
                                        alternativaDaPergunta2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void deve_permitir_um_segundo_quiz_inativo_no_mesmo_clube() {
        jdbc.update(
                "INSERT INTO quiz (id, clube_id, livro_id, criado_por, titulo, recompensa_xp,"
                        + " ativo) VALUES (gen_random_uuid(), ?, ?, ?, 'Quiz antigo', 50, false)",
                CLUBE_3,
                O_HOBBIT,
                USUARIO_DEMO);
    }

    @Test
    @Transactional
    void deve_recusar_um_segundo_quiz_ativo_no_mesmo_clube() {
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO quiz (id, clube_id, livro_id, criado_por,"
                                                + " titulo, recompensa_xp, ativo) VALUES"
                                                + " (gen_random_uuid(), ?, ?, ?, 'Quiz duplicado', 50,"
                                                + " true)",
                                        CLUBE_3,
                                        O_HOBBIT,
                                        USUARIO_DEMO))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private String cargoDe(UUID clubeId, UUID usuarioId) {
        return jdbc.queryForObject(
                "SELECT cargo_clube::text FROM membro_clube WHERE clube_id = ? AND usuario_id = ?",
                String.class,
                clubeId,
                usuarioId);
    }
}
