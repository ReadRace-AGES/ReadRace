package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Quiz do clube: POST /api/clubes/{clubeId}/quiz")
class PublicarQuizControllerIT {

    // Seed V13: o usuário fixo é administrador dos clubes 1 e 2, membro comum do 3 (que já tem
    // quiz ativo) e não participa do 4. Cada teste desfaz suas alterações por rollback.
    private static final String CLUBE_1 = "50000000-0000-0000-0000-000000000001";
    private static final String CLUBE_3 = "50000000-0000-0000-0000-000000000003";
    private static final String CLUBE_4 = "50000000-0000-0000-0000-000000000004";
    private static final String CLUBE_INEXISTENTE = "50000000-0000-0000-0000-0000000000ff";
    private static final String URL = "/api/clubes/{clubeId}/quiz";
    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-f-]{36})\"");

    private static final String PERGUNTA_VALIDA =
            """
            {"enunciado":"Quem escreveu?","alternativas":["A","B","C","D"],"correta":2}""";

    @Autowired private MockMvcTester mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    @AfterEach
    void deve_persistir_alteracoes_antes_do_rollback() {
        entityManager.flush();
    }

    @Test
    @DisplayName("publica o quiz com as perguntas e as 4 alternativas, e responde 201")
    void deve_publicar_o_quiz_inteiro() {
        String corpo =
                """
                {"titulo":"  Feitiços e Poções - Ano 1  ","recompensaXp":100,"perguntas":[
                  {"enunciado":"Primeira?","alternativas":["a1","a2","a3","a4"],"correta":0},
                  {"enunciado":"Segunda?","alternativas":["b1","b2","b3","b4"],"correta":3}]}""";

        MvcTestResult resposta = publicar(CLUBE_1, corpo);

        assertThat(resposta).hasStatus(HttpStatus.CREATED);
        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.titulo")
                .isEqualTo("Feitiços e Poções - Ano 1");
        assertThat(resposta).bodyJson().extractingPath("$.totalPerguntas").isEqualTo(2);

        UUID quizId = UUID.fromString(idDoQuiz(resposta));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT recompensa_xp FROM quiz WHERE id = ?",
                                Integer.class,
                                quizId))
                .isEqualTo(100);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT ativo FROM quiz WHERE id = ?", Boolean.class, quizId))
                .isTrue();
        // O quiz é sempre sobre o livro atual do clube.
        assertThat(
                        jdbc.queryForObject(
                                "SELECT q.livro_id = c.livro_id FROM quiz q"
                                        + " JOIN clube_do_livro c ON c.id = q.clube_id"
                                        + " WHERE q.id = ?",
                                Boolean.class,
                                quizId))
                .isTrue();
        assertThat(
                        jdbc.queryForList(
                                "SELECT ordem FROM pergunta_quiz WHERE quiz_id = ? ORDER BY ordem",
                                Integer.class,
                                quizId))
                .containsExactly(1, 2);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM alternativa_quiz a"
                                        + " JOIN pergunta_quiz p ON p.id = a.pergunta_id"
                                        + " WHERE p.quiz_id = ?",
                                Integer.class,
                                quizId))
                .isEqualTo(8);
        // Uma correta por pergunta: a primeira é a A, a segunda é a D.
        assertThat(
                        jdbc.queryForList(
                                "SELECT a.letra FROM alternativa_quiz a"
                                        + " JOIN pergunta_quiz p ON p.id = a.pergunta_id"
                                        + " WHERE p.quiz_id = ? AND a.correta ORDER BY p.ordem",
                                String.class,
                                quizId))
                .containsExactly("A", "D");
    }

    @Test
    @DisplayName("publicar de novo substitui o ativo: o antigo continua no banco, inativo")
    void deve_substituir_o_quiz_ativo() {
        String corpo = corpo("Quiz novo", 50, PERGUNTA_VALIDA);

        UUID primeiro = UUID.fromString(idDoQuiz(publicar(CLUBE_1, corpo)));
        UUID segundo = UUID.fromString(idDoQuiz(publicar(CLUBE_1, corpo)));

        assertThat(ativo(primeiro)).isFalse();
        assertThat(ativo(segundo)).isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM quiz WHERE clube_id = ?::uuid AND ativo",
                                Integer.class,
                                CLUBE_1))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM pergunta_quiz WHERE quiz_id = ?",
                                Integer.class,
                                primeiro))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("aceita os limites: recompensa 10 e 500, título de 120 caracteres")
    void deve_aceitar_os_limites() {
        assertThat(publicar(CLUBE_1, corpo("t".repeat(120), 10, PERGUNTA_VALIDA)))
                .hasStatus(HttpStatus.CREATED);
        assertThat(publicar(CLUBE_1, corpo("Quiz", 500, PERGUNTA_VALIDA)))
                .hasStatus(HttpStatus.CREATED);
    }

    @ParameterizedTest(name = "membro comum e quem não participa recebem 403: clube {0}")
    @ValueSource(strings = {CLUBE_3, CLUBE_4})
    void deve_recusar_quem_nao_e_administrador(String clubeId) {
        int quizzesAntes = totalDeQuizzes(clubeId);

        MvcTestResult resposta = publicar(clubeId, corpo("Quiz", 100, PERGUNTA_VALIDA));

        assertThat(resposta).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("SO_LIDER_CRIA_QUIZ");
        // Nada é gravado e o quiz ativo do clube 3 continua o mesmo.
        assertThat(totalDeQuizzes(clubeId)).isEqualTo(quizzesAntes);
    }

    @Test
    @DisplayName("clube inexistente responde 404 CLUBE_NAO_ENCONTRADO")
    void deve_responder_404_sem_clube() {
        MvcTestResult resposta =
                publicar(CLUBE_INEXISTENTE, corpo("Quiz", 100, PERGUNTA_VALIDA));

        assertThat(resposta).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("CLUBE_NAO_ENCONTRADO");
    }

    @ParameterizedTest(name = "corpo inválido responde 400 QUIZ_INVALIDO e não grava nada: {0}")
    @ValueSource(
            strings = {
                """
                {"titulo":"","recompensaXp":100,"perguntas":[%s]}""",
                """
                {"titulo":"   ","recompensaXp":100,"perguntas":[%s]}""",
                """
                {"recompensaXp":100,"perguntas":[%s]}""",
                """
                {"titulo":"Quiz","recompensaXp":9,"perguntas":[%s]}""",
                """
                {"titulo":"Quiz","recompensaXp":501,"perguntas":[%s]}""",
                """
                {"titulo":"Quiz","perguntas":[%s]}""",
                """
                {"titulo":"Quiz","recompensaXp":100,"perguntas":[]}""",
                """
                {"titulo":"Quiz","recompensaXp":100}""",
                """
                {"titulo":"Quiz","recompensaXp":100,"perguntas":[
                  {"enunciado":"?","alternativas":["A","","C","D"],"correta":0}]}""",
                """
                {"titulo":"Quiz","recompensaXp":100,"perguntas":[
                  {"enunciado":"?","alternativas":["A","B","C"],"correta":0}]}""",
                """
                {"titulo":"Quiz","recompensaXp":100,"perguntas":[
                  {"enunciado":"?","alternativas":["A","B","C","D","E"],"correta":0}]}""",
                """
                {"titulo":"Quiz","recompensaXp":100,"perguntas":[
                  {"enunciado":"?","alternativas":["A","B","C","D"],"correta":4}]}""",
                """
                {"titulo":"Quiz","recompensaXp":100,"perguntas":[
                  {"enunciado":"?","alternativas":["A","B","C","D"],"correta":-1}]}""",
                """
                {"titulo":"Quiz","recompensaXp":100,"perguntas":[
                  {"enunciado":"?","alternativas":["A","B","C","D"]}]}""",
                """
                {"titulo":"Quiz","recompensaXp":100,"perguntas":[
                  {"enunciado":"  ","alternativas":["A","B","C","D"],"correta":0}]}"""
            })
    void deve_recusar_quiz_invalido_sem_gravar(String modelo) {
        int quizzesAntes = totalDeQuizzes(CLUBE_1);
        String corpo = modelo.contains("%s") ? modelo.formatted(PERGUNTA_VALIDA) : modelo;

        MvcTestResult resposta = publicar(CLUBE_1, corpo);

        assertThat(resposta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(resposta).bodyJson().extractingPath("$.code").isEqualTo("QUIZ_INVALIDO");
        assertThat(totalDeQuizzes(CLUBE_1)).isEqualTo(quizzesAntes);
    }

    @Test
    @DisplayName("título com mais de 120 caracteres responde 400 QUIZ_INVALIDO")
    void deve_recusar_titulo_longo() {
        MvcTestResult resposta = publicar(CLUBE_1, corpo("t".repeat(121), 100, PERGUNTA_VALIDA));

        assertThat(resposta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(resposta).bodyJson().extractingPath("$.code").isEqualTo("QUIZ_INVALIDO");
    }

    private MvcTestResult publicar(String clubeId, String corpo) {
        return mvc.post()
                .uri(URL, clubeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo)
                .exchange();
    }

    private static String corpo(String titulo, int recompensaXp, String pergunta) {
        return "{\"titulo\":\"%s\",\"recompensaXp\":%d,\"perguntas\":[%s]}"
                .formatted(titulo, recompensaXp, pergunta);
    }

    private String idDoQuiz(MvcTestResult resposta) {
        assertThat(resposta).hasStatus(HttpStatus.CREATED);
        Matcher achou = ID.matcher(corpoDaResposta(resposta));
        assertThat(achou.find()).as("o corpo traz o id do quiz").isTrue();
        return achou.group(1);
    }

    private static String corpoDaResposta(MvcTestResult resposta) {
        try {
            return resposta.getResponse().getContentAsString();
        } catch (java.io.UnsupportedEncodingException erro) {
            throw new IllegalStateException(erro);
        }
    }

    private boolean ativo(UUID quizId) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject("SELECT ativo FROM quiz WHERE id = ?", Boolean.class, quizId));
    }

    private int totalDeQuizzes(String clubeId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM quiz WHERE clube_id = ?::uuid", Integer.class, clubeId);
    }
}
