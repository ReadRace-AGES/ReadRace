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
@DisplayName("Mascotes: GET /api/me/mascotes, PUT /api/me/mascote e o mascote do perfil")
class MascoteControllerIT {

    // Seed: o usuário fixo está no nível 7 e nunca escolheu mascote. V15: os 5 mascotes
    // desbloqueiam nos níveis 1, 3, 5, 7 e 9. Cada teste desfaz suas alterações por rollback.
    private static final UUID USUARIO_FIXO =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OUTRO_USUARIO =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String O_BRUXO = "b0000000-0000-0000-0000-000000000001";
    private static final String O_CINZENTO = "b0000000-0000-0000-0000-000000000002";
    private static final String A_BRUXA_MA = "b0000000-0000-0000-0000-000000000004";
    private static final String A_BRUXA_BOA = "b0000000-0000-0000-0000-000000000005";

    @Autowired private MockMvcTester mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    @AfterEach
    void deve_persistir_alteracoes_antes_do_rollback() {
        entityManager.flush();
    }

    @Test
    @DisplayName("lista os 5 na ordem da tela, com nível mínimo e imagem")
    void deve_listar_os_cinco_na_ordem() {
        MvcTestResult resposta = mvc.get().uri("/api/me/mascotes").exchange();

        assertThat(resposta).hasStatusOk();
        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.mascotes[*].nome")
                .asArray()
                .containsExactly(
                        "O Bruxo", "O Cinzento", "O Sonhador", "A Bruxa Má", "A Bruxa Boa");
        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.mascotes[*].nivelMinimo")
                .asArray()
                .containsExactly(1, 3, 5, 7, 9);
        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.mascotes[*].imagem")
                .asArray()
                .containsExactly(
                        "o-bruxo", "o-cinzento", "o-sonhador", "a-bruxa-ma", "a-bruxa-boa");
    }

    @Test
    @DisplayName("no nível 6, três desbloqueados e os dois últimos bloqueados")
    void deve_desbloquear_pelo_nivel() {
        jdbc.update("UPDATE usuario SET nivel = 6 WHERE id = ?", USUARIO_FIXO);

        assertThat(mvc.get().uri("/api/me/mascotes"))
                .bodyJson()
                .extractingPath("$.mascotes[*].desbloqueado")
                .asArray()
                .containsExactly(true, true, true, false, false);
    }

    @Test
    @DisplayName("no nível 7 do seed, quatro desbloqueados")
    void deve_desbloquear_quatro_no_nivel_do_seed() {
        assertThat(mvc.get().uri("/api/me/mascotes"))
                .bodyJson()
                .extractingPath("$.mascotes[*].desbloqueado")
                .asArray()
                .containsExactly(true, true, true, true, false);
    }

    @Test
    @DisplayName("quem nunca escolheu tem O Bruxo como escolhido e no perfil")
    void deve_usar_o_bruxo_como_padrao() {
        assertThat(mvc.get().uri("/api/me/mascotes"))
                .bodyJson()
                .extractingPath("$.mascotes[*].escolhido")
                .asArray()
                .containsExactly(true, false, false, false, false);

        MvcTestResult perfil = mvc.get().uri("/api/me/perfil").exchange();
        assertThat(perfil).bodyJson().extractingPath("$.mascote.nome").isEqualTo("O Bruxo");
        assertThat(perfil).bodyJson().extractingPath("$.mascote.imagem").isEqualTo("o-bruxo");
    }

    @Test
    @DisplayName("escolher um desbloqueado grava e o perfil passa a mostrar ele")
    void deve_escolher_mascote_desbloqueado() {
        MvcTestResult resposta = escolher(O_CINZENTO);

        assertThat(resposta).hasStatusOk();
        assertThat(resposta).bodyJson().extractingPath("$.id").isEqualTo(O_CINZENTO);
        assertThat(resposta).bodyJson().extractingPath("$.nome").isEqualTo("O Cinzento");
        assertThat(resposta).bodyJson().extractingPath("$.imagem").isEqualTo("o-cinzento");
        entityManager.flush();
        assertThat(mascoteNoBanco()).isEqualTo(UUID.fromString(O_CINZENTO));

        entityManager.clear();
        assertThat(mvc.get().uri("/api/me/perfil"))
                .bodyJson()
                .extractingPath("$.mascote.nome")
                .isEqualTo("O Cinzento");
        assertThat(mvc.get().uri("/api/me/mascotes"))
                .bodyJson()
                .extractingPath("$.mascotes[*].escolhido")
                .asArray()
                .containsExactly(false, true, false, false, false);
    }

    @Test
    @DisplayName("o mascote do próprio nível mínimo já pode ser escolhido")
    void deve_aceitar_mascote_no_nivel_exato() {
        assertThat(escolher(A_BRUXA_MA)).hasStatusOk();
    }

    @Test
    @DisplayName("mascote acima do nível responde 409 MASCOTE_BLOQUEADO e nada muda")
    void deve_recusar_mascote_bloqueado() {
        MvcTestResult resposta = escolher(A_BRUXA_BOA);

        assertThat(resposta).hasStatus(HttpStatus.CONFLICT);
        assertThat(resposta).bodyJson().extractingPath("$.code").isEqualTo("MASCOTE_BLOQUEADO");
        entityManager.flush();
        assertThat(mascoteNoBanco()).isNull();
    }

    @Test
    @DisplayName("mascote inexistente responde 404 MASCOTE_NAO_ENCONTRADO")
    void deve_responder_404_para_mascote_inexistente() {
        MvcTestResult resposta = escolher(UUID.randomUUID().toString());

        assertThat(resposta).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(resposta)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("MASCOTE_NAO_ENCONTRADO");
    }

    @Test
    @DisplayName("sem mascoteId responde 400")
    void deve_validar_corpo() {
        MvcTestResult resposta =
                mvc.put()
                        .uri("/api/me/mascote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .exchange();

        assertThat(resposta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(resposta).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("o perfil de outro usuário não traz o campo mascote")
    void perfil_de_outro_usuario_nao_tem_mascote() {
        assertThat(mvc.get().uri("/api/usuarios/" + OUTRO_USUARIO + "/perfil"))
                .bodyJson()
                .doesNotHavePath("$.mascote");
    }

    @Test
    @DisplayName("escolher de novo o padrão grava ele explicitamente")
    void deve_permitir_voltar_ao_padrao() {
        escolher(O_CINZENTO);
        assertThat(escolher(O_BRUXO)).hasStatusOk();
        entityManager.flush();
        assertThat(mascoteNoBanco()).isEqualTo(UUID.fromString(O_BRUXO));
    }

    private MvcTestResult escolher(String mascoteId) {
        return mvc.put()
                .uri("/api/me/mascote")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mascoteId\":\"%s\"}".formatted(mascoteId))
                .exchange();
    }

    private UUID mascoteNoBanco() {
        return jdbc.queryForObject(
                "SELECT mascote_id FROM usuario WHERE id = ?", UUID.class, USUARIO_FIXO);
    }
}
