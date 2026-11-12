package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
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
import org.springframework.test.json.AbstractJsonContentAssert;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.model.SequenciaDeLeitura;
import com.readrace.api.repository.ConquistaRepository;

import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Recompensa só com leitura de verdade (#199)")
class LeituraDeVerdadeIT {

    // Seed: o Daniel (usuário atual) parou Dom Casmurro na página 145 de 256, tem 5 livros lidos
    // até a última página (A Metamorfose entre eles) e já tem a conquista Leitor Dedicado. O Hobbit
    // (300 páginas) está na lista de desejos, na página 0. Tudo é desfeito por rollback.
    private static final UUID DANIEL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ANA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static final UUID DOM_CASMURRO_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID ORGULHO_E_PRECONCEITO_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID A_METAMORFOSE_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final UUID FRANKENSTEIN_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000005");
    private static final UUID A_HORA_DA_ESTRELA_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000007");
    private static final UUID O_HOBBIT_ID = UUID.fromString("30000000-0000-0000-0000-000000000008");
    private static final UUID O_PEQUENO_PRINCIPE_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000009");
    private static final UUID DORIAN_GRAY_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000011");
    private static final UUID ENSAIO_SOBRE_A_CEGUEIRA_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000012");

    private static final String LEITOR_DEDICADO = "Leitor Dedicado";

    @Autowired private MockMvcTester mvc;

    @Autowired private JdbcTemplate jdbc;

    @Autowired private EntityManager entityManager;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private ConquistaRepository conquistaRepository;

    private final LocalDate hoje = SequenciaDeLeitura.hoje();

    @Test
    @DisplayName(
            "livro \"Lido\" pelo catálogo na página 0: XP de página e nenhum bônus de conclusão")
    void nao_deve_pagar_conclusao_para_livro_marcado_como_lido_pelo_catalogo() {
        definirItem(O_HOBBIT_ID, "lido", 0);

        registrarComResposta(O_HOBBIT_ID, 1)
                .extractingPath("$")
                .asMap()
                .containsEntry("xpPaginas", 1)
                .containsEntry("xpConclusao", 0)
                .containsEntry("xpTotal", 1)
                .containsEntry("concluido", false);
        registrarComResposta(O_HOBBIT_ID, 2)
                .extractingPath("$")
                .asMap()
                .containsEntry("xpPaginas", 1)
                .containsEntry("xpConclusao", 0)
                .containsEntry("xpTotal", 1);
    }

    @Test
    @DisplayName("chegar à última página paga o bônus de conclusão uma vez")
    void deve_pagar_conclusao_uma_vez_ao_chegar_na_ultima_pagina() {
        definirItem(O_HOBBIT_ID, "lendo", 280);

        registrarComResposta(O_HOBBIT_ID, 300)
                .extractingPath("$")
                .asMap()
                .containsEntry("xpPaginas", 20)
                .containsEntry("xpConclusao", 150)
                .containsEntry("xpTotal", 170)
                .containsEntry("concluido", true);
    }

    @Test
    @DisplayName("livro já concluído: voltar à página 100 e registrar a 300 de novo não paga nada")
    void nao_deve_pagar_nada_ao_registrar_de_novo_um_livro_ja_concluido() {
        definirItem(O_HOBBIT_ID, "lendo", 280);
        registrar(O_HOBBIT_ID, 300);
        int xpAntes = xp();

        registrarComResposta(O_HOBBIT_ID, 100).extractingPath("$.xpTotal").isEqualTo(0);
        registrarComResposta(O_HOBBIT_ID, 300)
                .extractingPath("$")
                .asMap()
                .containsEntry("xpPaginas", 0)
                .containsEntry("xpConclusao", 0)
                .containsEntry("xpTotal", 0);

        assertThat(xp()).isEqualTo(xpAntes);
    }

    @Test
    @DisplayName("perfil e conquista contam só os livros lidos até a última página")
    void deve_contar_no_perfil_e_na_conquista_so_os_livros_lidos_ate_o_fim() {
        manterSoLivros(ORGULHO_E_PRECONCEITO_ID, A_METAMORFOSE_ID);
        for (UUID livroId : List.of(FRANKENSTEIN_ID, O_HOBBIT_ID, DORIAN_GRAY_ID)) {
            definirItem(livroId, "lido", 0);
        }

        assertThat(mvc.get().uri("/api/me/perfil"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.estatisticas.livrosLidos")
                .isEqualTo(2);
        assertThat(conquistaRepository.contarLivrosLidos(DANIEL_ID)).isEqualTo(2);
    }

    @Test
    @DisplayName("a 1 livro da conquista: marcar \"Lido\" pelo catálogo não a desbloqueia")
    void nao_deve_desbloquear_a_conquista_de_livros_lidos_pelo_catalogo() {
        apagarConquista(LEITOR_DEDICADO);
        marcarComoLendo(O_PEQUENO_PRINCIPE_ID);
        definirItem(O_HOBBIT_ID, "lido", 0);
        int xpAntes = xp();

        registrarComResposta(DOM_CASMURRO_ID, 150)
                .extractingPath("$.conquistasDesbloqueadas[*].nome")
                .asArray()
                .doesNotContain(LEITOR_DEDICADO);

        assertThat(conquistas()).doesNotContain(LEITOR_DEDICADO);
        // Só as 5 páginas novas (145 -> 150) rendem XP; a recompensa da conquista não é paga.
        assertThat(xp()).isEqualTo(xpAntes + 5);
    }

    @Test
    @DisplayName("sequência: registrar a mesma página, ou uma menor, não conta como dia lido")
    void nao_deve_subir_a_sequencia_sem_pagina_nova() {
        definirSequencia(4, hoje.minusDays(1));

        registrarComResposta(DOM_CASMURRO_ID, 145).extractingPath("$.sequenciaDias").isEqualTo(4);
        registrarComResposta(DOM_CASMURRO_ID, 100).extractingPath("$.sequenciaDias").isEqualTo(4);

        assertThat(sequenciaNoBanco()).isEqualTo(4);
        assertThat(ultimaLeituraNoBanco()).isEqualTo(hoje.minusDays(1));
    }

    @Test
    @DisplayName("sequência: com página nova, continua subindo")
    void deve_subir_a_sequencia_com_pagina_nova() {
        definirSequencia(4, hoje.minusDays(1));

        registrarComResposta(DOM_CASMURRO_ID, 146).extractingPath("$.sequenciaDias").isEqualTo(5);
    }

    @Test
    @DisplayName("desafio de livro: registrar a última página de um livro já lido não vence")
    void nao_deve_vencer_o_desafio_de_livro_ja_lido_ate_o_fim() throws Exception {
        UUID desafioId = criarDesafioDeLivro(A_METAMORFOSE_ID);

        registrar(A_METAMORFOSE_ID, 128);

        assertThat(statusDoDesafio(desafioId)).isEqualTo("ativo");
    }

    @Test
    @DisplayName("conquista já ganha continua com o usuário com a regra nova")
    void deve_manter_a_conquista_ja_ganha() {
        marcarComoLendo(O_PEQUENO_PRINCIPE_ID, A_HORA_DA_ESTRELA_ID, ENSAIO_SOBRE_A_CEGUEIRA_ID);

        registrar(DOM_CASMURRO_ID, 150);

        assertThat(conquistas()).contains(LEITOR_DEDICADO);
    }

    private void registrar(UUID livroId, int pagina) {
        registrarComResposta(livroId, pagina);
    }

    private AbstractJsonContentAssert<?> registrarComResposta(UUID livroId, int pagina) {
        return assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", livroId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":" + pagina + "}"))
                .hasStatusOk()
                .bodyJson();
    }

    private UUID criarDesafioDeLivro(UUID livroId) throws Exception {
        MvcTestResult resultado =
                mvc.post()
                        .uri("/api/desafios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"oponenteId": "%s", "tipoMeta": "livro", "livroId": "%s", "prazoDias": 7}
                                """
                                        .formatted(ANA_ID, livroId))
                        .exchange();

        assertThat(resultado).hasStatus(HttpStatus.CREATED);

        return UUID.fromString(
                objectMapper
                        .readTree(resultado.getResponse().getContentAsString())
                        .path("id")
                        .textValue());
    }

    // O SQL direto passa por fora do Hibernate: o flush grava o que ele tem pendente antes, e o
    // clear descarta as entidades que ficariam desatualizadas depois.
    private void executar(String sql, Object... parametros) {
        entityManager.flush();
        jdbc.update(sql, parametros);
        entityManager.clear();
    }

    /** Cria ou substitui o item do Daniel, como o catálogo ou o registro o deixariam. */
    private void definirItem(UUID livroId, String status, int pagina) {
        executar(
                """
                INSERT INTO item_biblioteca
                    (id, usuario_id, livro_id, status_leitura, pagina_atual, pagina_maxima)
                VALUES (?, ?, ?, ?::status_leitura, ?, ?)
                ON CONFLICT (usuario_id, livro_id) DO UPDATE
                SET status_leitura = EXCLUDED.status_leitura,
                    pagina_atual = EXCLUDED.pagina_atual,
                    pagina_maxima = EXCLUDED.pagina_maxima
                """,
                UUID.randomUUID(),
                DANIEL_ID,
                livroId,
                status,
                pagina,
                pagina);
    }

    private void marcarComoLendo(UUID... livroIds) {
        for (UUID livroId : livroIds) {
            executar(
                    "UPDATE item_biblioteca SET status_leitura = 'lendo'"
                            + " WHERE usuario_id = ? AND livro_id = ?",
                    DANIEL_ID,
                    livroId);
        }
    }

    private void manterSoLivros(UUID... livroIds) {
        String outrosItens =
                "SELECT id FROM item_biblioteca WHERE usuario_id = ? AND livro_id NOT IN (?, ?)";
        Object[] parametros = {DANIEL_ID, livroIds[0], livroIds[1]};
        executar(
                "DELETE FROM registro_leitura WHERE item_biblioteca_id IN (" + outrosItens + ")",
                parametros);
        executar("DELETE FROM item_biblioteca WHERE id IN (" + outrosItens + ")", parametros);
    }

    private void apagarConquista(String nome) {
        executar(
                "DELETE FROM conquista_usuario WHERE usuario_id = ?"
                        + " AND conquista_id = (SELECT id FROM conquista WHERE nome = ?)",
                DANIEL_ID,
                nome);
    }

    private void definirSequencia(int dias, LocalDate ultimaLeitura) {
        executar(
                "UPDATE usuario SET dias_consecutivos = ?, ultima_leitura_em = ? WHERE id = ?",
                dias,
                ultimaLeitura,
                DANIEL_ID);
    }

    private List<String> conquistas() {
        entityManager.flush();
        return jdbc.queryForList(
                "SELECT c.nome FROM conquista_usuario cu JOIN conquista c ON c.id = cu.conquista_id"
                        + " WHERE cu.usuario_id = ?",
                String.class,
                DANIEL_ID);
    }

    private int xp() {
        entityManager.flush();
        return jdbc.queryForObject(
                "SELECT xp_total FROM usuario WHERE id = ?", Integer.class, DANIEL_ID);
    }

    private int sequenciaNoBanco() {
        entityManager.flush();
        return jdbc.queryForObject(
                "SELECT dias_consecutivos FROM usuario WHERE id = ?", Integer.class, DANIEL_ID);
    }

    private LocalDate ultimaLeituraNoBanco() {
        entityManager.flush();
        return jdbc.queryForObject(
                "SELECT ultima_leitura_em FROM usuario WHERE id = ?", LocalDate.class, DANIEL_ID);
    }

    private String statusDoDesafio(UUID desafioId) {
        entityManager.flush();
        return jdbc.queryForObject(
                "SELECT status::text FROM desafio_amigo WHERE id = ?", String.class, desafioId);
    }
}
