package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
@DisplayName("Quiz do clube: GET /quiz e POST /quiz/perguntas/{perguntaId}/resposta")
class QuizControllerIT {

    // Seed V13: o usuário fixo é membro comum do clube 3, que tem o quiz de O Hobbit com 3
    // perguntas (a correta é A, B e A). Ele é administrador do clube 1 e não participa do 4.
    // Cada teste desfaz suas alterações por rollback.
    private static final String CLUBE_1 = "50000000-0000-0000-0000-000000000001";
    private static final String CLUBE_3 = "50000000-0000-0000-0000-000000000003";
    private static final String CLUBE_4 = "50000000-0000-0000-0000-000000000004";
    private static final UUID USUARIO_FIXO =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final String PERGUNTA_1 = "72000000-0000-0000-0000-000000000001";
    private static final String PERGUNTA_2 = "72000000-0000-0000-0000-000000000002";
    private static final String PERGUNTA_3 = "72000000-0000-0000-0000-000000000003";

    private static final String P1_CORRETA = "73000000-0000-0000-0000-000000000001";
    private static final String P1_ERRADA = "73000000-0000-0000-0000-000000000002";
    private static final String P2_ERRADA = "73000000-0000-0000-0000-000000000005";
    private static final String P3_CORRETA = "73000000-0000-0000-0000-000000000009";

    private static final String URL_QUIZ = "/api/clubes/{clubeId}/quiz";
    private static final String URL_RESPOSTA =
            "/api/clubes/{clubeId}/quiz/perguntas/{perguntaId}/resposta";

    @Autowired private MockMvcTester mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    @AfterEach
    void deve_persistir_alteracoes_antes_do_rollback() {
        entityManager.flush();
    }

    @Test
    @DisplayName("abre na primeira pergunta, com o título do livro e 4 alternativas")
    void deve_abrir_na_primeira_pergunta() {
        MvcTestResult resposta = mvc.get().uri(URL_QUIZ, CLUBE_3).exchange();

        assertThat(resposta).hasStatusOk();
        assertThat(resposta).bodyJson().extractingPath("$.livro.titulo").isEqualTo("O Hobbit");
        assertThat(resposta).bodyJson().extractingPath("$.titulo").isEqualTo("Quiz sobre O Hobbit");
        assertThat(resposta).bodyJson().extractingPath("$.recompensaXp").isEqualTo(100);
        assertThat(resposta).bodyJson().extractingPath("$.totalPerguntas").isEqualTo(3);
        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.proximaPergunta.id")
                .isEqualTo(PERGUNTA_1);
        assertThat(resposta).bodyJson().extractingPath("$.proximaPergunta.numero").isEqualTo(1);
        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.proximaPergunta.alternativas[*].letra")
                .asArray()
                .containsExactly("A", "B", "C", "D");
    }

    @Test
    @DisplayName("a alternativa correta nunca vai para o app antes da resposta")
    void nao_deve_revelar_a_alternativa_correta() {
        assertThat(mvc.get().uri(URL_QUIZ, CLUBE_3))
                .hasStatusOk()
                .bodyJson()
                .doesNotHavePath("$.proximaPergunta.alternativas[0].correta");
    }

    @Test
    @DisplayName("acerto: +50 Pontos no clube do quiz")
    void deve_somar_50_pontos_no_acerto() {
        int antes = pontosNoClube3();

        MvcTestResult resposta = responder(PERGUNTA_1, P1_CORRETA);

        assertThat(resposta).hasStatusOk();
        assertThat(resposta).bodyJson().extractingPath("$.acertou").isEqualTo(true);
        assertThat(resposta).bodyJson().extractingPath("$.pontosGanhos").isEqualTo(50);
        assertThat(resposta).bodyJson().extractingPath("$.concluiu").isEqualTo(false);
        assertThat(resposta).bodyJson().extractingPath("$.resumo").isNull();
        assertThat(pontosNoClube3()).isEqualTo(antes + 50);
    }

    @Test
    @DisplayName("erro: 0 Pontos e a resposta não diz qual era a correta")
    void nao_deve_somar_pontos_no_erro() {
        int antes = pontosNoClube3();

        MvcTestResult resposta = responder(PERGUNTA_1, P1_ERRADA);

        assertThat(resposta).hasStatusOk();
        assertThat(resposta).bodyJson().extractingPath("$.acertou").isEqualTo(false);
        assertThat(resposta).bodyJson().extractingPath("$.pontosGanhos").isEqualTo(0);
        assertThat(resposta).bodyJson().doesNotHavePath("$.alternativaCorreta");
        assertThat(pontosNoClube3()).isEqualTo(antes);
    }

    @Test
    @DisplayName("sair depois da primeira pergunta e voltar abre direto na segunda")
    void deve_continuar_da_proxima_pergunta_nao_respondida() {
        assertThat(responder(PERGUNTA_1, P1_CORRETA)).hasStatusOk();

        assertThat(mvc.get().uri(URL_QUIZ, CLUBE_3))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.proximaPergunta.numero")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("responder a última conclui o quiz, devolve o resumo e paga o XP uma vez")
    void deve_concluir_o_quiz_na_ultima_pergunta() {
        int xpAntes = xpDoUsuarioFixo();
        int pontosAntes = pontosNoClube3();

        assertThat(responder(PERGUNTA_1, P1_CORRETA)).hasStatusOk();
        assertThat(responder(PERGUNTA_2, P2_ERRADA)).hasStatusOk();
        MvcTestResult ultima = responder(PERGUNTA_3, P3_CORRETA);

        assertThat(ultima).hasStatusOk();
        assertThat(ultima).bodyJson().extractingPath("$.concluiu").isEqualTo(true);
        assertThat(ultima).bodyJson().extractingPath("$.resumo.acertos").isEqualTo(2);
        assertThat(ultima).bodyJson().extractingPath("$.resumo.total").isEqualTo(3);
        assertThat(ultima).bodyJson().extractingPath("$.resumo.pontos").isEqualTo(100);
        assertThat(ultima).bodyJson().extractingPath("$.resumo.xp").isEqualTo(100);

        entityManager.flush();
        assertThat(xpDoUsuarioFixo()).isEqualTo(xpAntes + 100);
        assertThat(pontosNoClube3()).isEqualTo(pontosAntes + 100);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM conclusao_quiz WHERE usuario_id = ?",
                                Integer.class,
                                USUARIO_FIXO))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("depois de concluir, proximaPergunta vem null")
    void deve_devolver_proxima_pergunta_null_quando_respondeu_todas() {
        responder(PERGUNTA_1, P1_CORRETA);
        responder(PERGUNTA_2, P2_ERRADA);
        responder(PERGUNTA_3, P3_CORRETA);

        assertThat(mvc.get().uri(URL_QUIZ, CLUBE_3))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.proximaPergunta")
                .isNull();
    }

    @Test
    @DisplayName("a mesma resposta enviada duas vezes: 409 e nenhum ponto somado de novo")
    void deve_recusar_a_segunda_resposta_da_mesma_pergunta() {
        int antes = pontosNoClube3();
        assertThat(responder(PERGUNTA_1, P1_CORRETA)).hasStatusOk();

        MvcTestResult segunda = responder(PERGUNTA_1, P1_CORRETA);

        assertThat(segunda).hasStatus(HttpStatus.CONFLICT);
        assertThat(segunda).bodyJson().extractingPath("$.code").isEqualTo("PERGUNTA_JA_RESPONDIDA");
        assertThat(pontosNoClube3()).isEqualTo(antes + 50);
    }

    @Test
    @DisplayName("administrador do clube recebe 403 no GET e no POST")
    void deve_recusar_o_administrador() {
        assertThat(mvc.get().uri(URL_QUIZ, CLUBE_1))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("SO_MEMBRO_RESPONDE");
        assertThat(
                        mvc.post()
                                .uri(URL_RESPOSTA, CLUBE_1, PERGUNTA_1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"alternativaId\":\"" + P1_CORRETA + "\"}"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("SO_MEMBRO_RESPONDE");
    }

    @Test
    @DisplayName("quem não participa do clube recebe 403")
    void deve_recusar_quem_nao_e_membro() {
        assertThat(mvc.get().uri(URL_QUIZ, CLUBE_4))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("SO_MEMBRO_RESPONDE");
    }

    @Test
    @DisplayName("clube sem quiz ativo responde 404 QUIZ_NAO_ENCONTRADO")
    void deve_responder_404_sem_quiz_ativo() {
        jdbc.update("UPDATE quiz SET ativo = false WHERE clube_id = ?::uuid", CLUBE_3);

        assertThat(mvc.get().uri(URL_QUIZ, CLUBE_3))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("QUIZ_NAO_ENCONTRADO");
    }

    @Test
    void deve_responder_404_para_clube_inexistente() {
        assertThat(mvc.get().uri(URL_QUIZ, UUID.randomUUID()))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void deve_responder_404_para_pergunta_fora_do_quiz() {
        assertThat(responder(UUID.randomUUID().toString(), P1_CORRETA))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("alternativa de outra pergunta: 400 e nada gravado")
    void deve_recusar_alternativa_de_outra_pergunta() {
        int antes = pontosNoClube3();

        assertThat(responder(PERGUNTA_2, P1_CORRETA))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("ALTERNATIVA_INVALIDA");
        assertThat(pontosNoClube3()).isEqualTo(antes);
    }

    @Test
    void deve_recusar_resposta_sem_alternativa() {
        assertThat(
                        mvc.post()
                                .uri(URL_RESPOSTA, CLUBE_3, PERGUNTA_1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("VALIDATION_ERROR");
    }

    private MvcTestResult responder(String perguntaId, String alternativaId) {
        return mvc.post()
                .uri(URL_RESPOSTA, CLUBE_3, perguntaId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"alternativaId\":\"" + alternativaId + "\"}")
                .exchange();
    }

    private int pontosNoClube3() {
        entityManager.flush();
        return jdbc.queryForObject(
                "SELECT pontos FROM membro_clube WHERE clube_id = ?::uuid AND usuario_id = ?",
                Integer.class,
                CLUBE_3,
                USUARIO_FIXO);
    }

    private int xpDoUsuarioFixo() {
        entityManager.flush();
        return jdbc.queryForObject(
                "SELECT xp_total FROM usuario WHERE id = ?", Integer.class, USUARIO_FIXO);
    }
}
