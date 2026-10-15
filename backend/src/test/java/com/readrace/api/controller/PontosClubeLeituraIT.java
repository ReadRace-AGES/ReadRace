package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.service.ConquistaService;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Pontos do clube pelo registro de leitura")
class PontosClubeLeituraIT {

    // Seed: o Daniel (usuário atual) é administrador do clube 1, que lê Dom Casmurro, e parou o
    // livro na página 145 de 256. Ele não participa do clube 4, que lê Crime e Castigo, livro em
    // que parou na página 310 de 620. Cada teste desfaz suas alterações por rollback.
    private static final UUID DANIEL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ANA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CLUBE_1_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID DOM_CASMURRO_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID CRIME_E_CASTIGO_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000006");

    @Autowired private MockMvcTester mvc;

    @Autowired private JdbcTemplate jdbc;

    @Autowired private EntityManager entityManager;

    @MockitoSpyBean private ConquistaService conquistaService;

    @Test
    void deve_somar_um_ponto_por_pagina_nova_no_clube_que_le_o_livro() {
        int pontosAntes = pontos(CLUBE_1_ID, DANIEL_ID);

        registrar(DOM_CASMURRO_ID, 175);

        assertThat(pontos(CLUBE_1_ID, DANIEL_ID)).isEqualTo(pontosAntes + 30);
    }

    @Test
    void nao_deve_somar_pontos_ao_registrar_pagina_abaixo_da_maior_ja_alcancada() {
        registrar(DOM_CASMURRO_ID, 175);
        int pontosAntes = pontos(CLUBE_1_ID, DANIEL_ID);

        registrar(DOM_CASMURRO_ID, 160);

        assertThat(pontos(CLUBE_1_ID, DANIEL_ID)).isEqualTo(pontosAntes);
    }

    @Test
    void deve_somar_os_pontos_so_na_linha_de_quem_leu() {
        int pontosDaAna = pontos(CLUBE_1_ID, ANA_ID);

        registrar(DOM_CASMURRO_ID, 175);

        assertThat(pontos(CLUBE_1_ID, ANA_ID)).isEqualTo(pontosDaAna);
    }

    @Test
    @DisplayName("dois clubes lendo o mesmo livro: administrador e membro ganham igual em cada um")
    void deve_somar_em_todos_os_clubes_do_usuario_que_leem_o_livro() {
        UUID segundoClubeId = criarClubeComDanielComoMembro(DOM_CASMURRO_ID);
        int pontosNoClube1 = pontos(CLUBE_1_ID, DANIEL_ID);

        registrar(DOM_CASMURRO_ID, 155);

        assertThat(pontos(CLUBE_1_ID, DANIEL_ID)).isEqualTo(pontosNoClube1 + 10);
        assertThat(pontos(segundoClubeId, DANIEL_ID)).isEqualTo(10);
    }

    @Test
    void nao_deve_somar_pontos_quando_o_livro_nao_e_de_nenhum_clube_do_usuario() {
        int totalAntes = totalDePontosDeTodosOsClubes();

        registrar(CRIME_E_CASTIGO_ID, 400);

        assertThat(totalDePontosDeTodosOsClubes()).isEqualTo(totalAntes);
    }

    @Test
    @DisplayName("concluir o livro paga só as páginas novas: o bônus de +150 é XP, não ponto")
    void nao_deve_transformar_o_bonus_de_conclusao_em_pontos() {
        int pontosAntes = pontos(CLUBE_1_ID, DANIEL_ID);

        MvcTestResult resposta = registrar(DOM_CASMURRO_ID, 256);

        assertThat(resposta).bodyJson().extractingPath("$.xpConclusao").isEqualTo(150);
        assertThat(pontos(CLUBE_1_ID, DANIEL_ID)).isEqualTo(pontosAntes + 111);
    }

    @Test
    void nao_deve_somar_pontos_em_clube_excluido() {
        int pontosAntes = pontos(CLUBE_1_ID, DANIEL_ID);
        entityManager.flush();
        jdbc.update("UPDATE clube_do_livro SET excluido_em = now() WHERE id = ?", CLUBE_1_ID);
        entityManager.clear();

        registrar(DOM_CASMURRO_ID, 175);

        assertThat(pontos(CLUBE_1_ID, DANIEL_ID)).isEqualTo(pontosAntes);
    }

    @Test
    void nao_deve_somar_pontos_quando_a_pagina_e_invalida() {
        int pontosAntes = pontos(CLUBE_1_ID, DANIEL_ID);

        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", DOM_CASMURRO_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":300}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY);

        assertThat(pontos(CLUBE_1_ID, DANIEL_ID)).isEqualTo(pontosAntes);
    }

    // Sem a transação do teste, o registro abre e desfaz a própria transação, como em produção.
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void deve_desfazer_os_pontos_quando_o_registro_falha_depois_de_soma_los() {
        int pontosAntes = pontosNoBanco(CLUBE_1_ID, DANIEL_ID);
        // A avaliação de conquistas é a última etapa do registro e grava o que está pendente:
        // quando ela falha, a soma dos pontos já tinha ido para o banco dentro da transação.
        doAnswer(
                        invocacao -> {
                            invocacao.callRealMethod();
                            throw new IllegalStateException(
                                    "Falha simulada depois de somar os pontos");
                        })
                .when(conquistaService)
                .avaliar(DANIEL_ID);

        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", DOM_CASMURRO_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":175}"))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(pontosNoBanco(CLUBE_1_ID, DANIEL_ID)).isEqualTo(pontosAntes);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT pagina_maxima FROM item_biblioteca"
                                        + " WHERE usuario_id = ? AND livro_id = ?",
                                Integer.class,
                                DANIEL_ID,
                                DOM_CASMURRO_ID))
                .isEqualTo(145);
    }

    private MvcTestResult registrar(UUID livroId, int pagina) {
        MvcTestResult resposta =
                mvc.post()
                        .uri("/api/livros/{livroId}/progresso", livroId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pagina\":" + pagina + "}")
                        .exchange();

        assertThat(resposta).hasStatusOk();

        return resposta;
    }

    // O SQL direto passa por fora do Hibernate: o flush grava o que ele tem pendente antes, e o
    // clear descarta as entidades que ficariam desatualizadas depois.
    private UUID criarClubeComDanielComoMembro(UUID livroId) {
        UUID clubeId = UUID.randomUUID();

        entityManager.flush();
        jdbc.update(
                "INSERT INTO clube_do_livro (id, livro_id, nome) VALUES (?, ?, 'Segundo clube')",
                clubeId,
                livroId);
        jdbc.update(
                "INSERT INTO membro_clube (id, clube_id, usuario_id, pontos, cargo_clube)"
                        + " VALUES (?, ?, ?, 0, 'membro'::cargo)",
                UUID.randomUUID(),
                clubeId,
                DANIEL_ID);
        entityManager.clear();

        return clubeId;
    }

    private int pontos(UUID clubeId, UUID usuarioId) {
        entityManager.flush();

        return pontosNoBanco(clubeId, usuarioId);
    }

    private int pontosNoBanco(UUID clubeId, UUID usuarioId) {
        return jdbc.queryForObject(
                "SELECT pontos FROM membro_clube WHERE clube_id = ? AND usuario_id = ?",
                Integer.class,
                clubeId,
                usuarioId);
    }

    private int totalDePontosDeTodosOsClubes() {
        entityManager.flush();

        return jdbc.queryForObject("SELECT sum(pontos) FROM membro_clube", Integer.class);
    }
}
