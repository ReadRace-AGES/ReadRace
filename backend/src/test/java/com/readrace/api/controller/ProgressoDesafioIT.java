package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Progresso e resultado dos desafios")
class ProgressoDesafioIT {

    // Seed: o Daniel (usuário atual) disputa um desafio ativo de 300 páginas contra a Ana e parou
    // Dom Casmurro na página 145 de 256. Cada teste desfaz suas alterações por rollback.
    private static final UUID DANIEL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ANA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID LUCAS_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID DESAFIO_ATIVO_ID =
            UUID.fromString("90000000-0000-0000-0000-000000000001");
    private static final UUID DESAFIO_FINALIZADO_ID =
            UUID.fromString("90000000-0000-0000-0000-000000000002");
    private static final UUID DESAFIO_PENDENTE_ID =
            UUID.fromString("90000000-0000-0000-0000-000000000004");
    private static final UUID DOM_CASMURRO_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID A_METAMORFOSE_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000004");

    @Autowired private MockMvcTester mvc;

    @Autowired private JdbcTemplate jdbc;

    @Autowired private EntityManager entityManager;

    @Autowired private ObjectMapper objectMapper;

    @Test
    void deve_somar_as_paginas_novas_so_no_placar_de_quem_leu() {
        definirPlacar(DESAFIO_ATIVO_ID, DANIEL_ID, 120);
        int placarDaAna = placar(DESAFIO_ATIVO_ID, ANA_ID);

        registrar(DOM_CASMURRO_ID, 200);

        assertThat(placar(DESAFIO_ATIVO_ID, DANIEL_ID)).isEqualTo(175);
        assertThat(placar(DESAFIO_ATIVO_ID, ANA_ID)).isEqualTo(placarDaAna);
    }

    @Test
    void deve_finalizar_com_vitoria_quando_o_placar_alcanca_a_meta() throws Exception {
        definirPlacar(DESAFIO_ATIVO_ID, DANIEL_ID, 280);

        registrar(DOM_CASMURRO_ID, 175);

        assertThat(statusNoBanco(DESAFIO_ATIVO_ID)).isEqualTo("finalizado");
        assertThat(statusNoDetalhe(DESAFIO_ATIVO_ID)).isEqualTo("concluido_ganho");
    }

    @Test
    void nao_deve_contar_paginas_de_outro_livro_no_desafio_de_livro() throws Exception {
        UUID desafioId = criarDesafioDeLivro(A_METAMORFOSE_ID);

        registrar(DOM_CASMURRO_ID, 200);

        assertThat(placar(desafioId, DANIEL_ID)).isZero();
        assertThat(statusNoBanco(desafioId)).isEqualTo("ativo");
    }

    @Test
    void deve_finalizar_com_vitoria_quando_o_livro_do_desafio_e_concluido() throws Exception {
        UUID desafioId = criarDesafioDeLivro(DOM_CASMURRO_ID);

        registrar(DOM_CASMURRO_ID, 256);

        assertThat(statusNoBanco(desafioId)).isEqualTo("finalizado");
        assertThat(statusNoDetalhe(desafioId)).isEqualTo("concluido_ganho");
    }

    @Test
    void deve_dar_vitoria_a_quem_conclui_o_livro_mesmo_com_menos_paginas_novas() throws Exception {
        UUID desafioId = criarDesafioDeLivro(DOM_CASMURRO_ID);
        definirPlacar(desafioId, ANA_ID, 200);

        // Só 111 páginas novas (145 a 256), mas concluir vale o livro inteiro.
        registrar(DOM_CASMURRO_ID, 256);

        assertThat(placar(desafioId, DANIEL_ID)).isEqualTo(256);
        assertThat(statusNoDetalhe(desafioId)).isEqualTo("concluido_ganho");
    }

    @Test
    void deve_finalizar_desafio_com_prazo_vencido_ao_listar() throws Exception {
        encerrarPrazo(DESAFIO_ATIVO_ID);
        definirPlacar(DESAFIO_ATIVO_ID, DANIEL_ID, 150);
        definirPlacar(DESAFIO_ATIVO_ID, ANA_ID, 90);

        assertThat(statusNaListagem(DESAFIO_ATIVO_ID)).isEqualTo("concluido_ganho");
        assertThat(statusNoBanco(DESAFIO_ATIVO_ID)).isEqualTo("finalizado");
    }

    @Test
    void deve_finalizar_desafio_com_prazo_vencido_ao_abrir_o_detalhe() throws Exception {
        encerrarPrazo(DESAFIO_ATIVO_ID);
        definirPlacar(DESAFIO_ATIVO_ID, DANIEL_ID, 90);
        definirPlacar(DESAFIO_ATIVO_ID, ANA_ID, 150);

        assertThat(statusNoDetalhe(DESAFIO_ATIVO_ID)).isEqualTo("concluido_perdido");
        assertThat(statusNoBanco(DESAFIO_ATIVO_ID)).isEqualTo("finalizado");
    }

    @Test
    void deve_finalizar_desafio_com_prazo_vencido_no_registro_sem_somar_paginas() {
        encerrarPrazo(DESAFIO_ATIVO_ID);
        definirPlacar(DESAFIO_ATIVO_ID, DANIEL_ID, 150);

        registrar(DOM_CASMURRO_ID, 200);

        assertThat(statusNoBanco(DESAFIO_ATIVO_ID)).isEqualTo("finalizado");
        assertThat(placar(DESAFIO_ATIVO_ID, DANIEL_ID)).isEqualTo(150);
    }

    @Test
    void deve_devolver_empate_sem_derrubar_a_listagem() throws Exception {
        definirPlacar(DESAFIO_FINALIZADO_ID, LUCAS_ID, 245);
        definirPlacar(DESAFIO_FINALIZADO_ID, DANIEL_ID, 245);

        assertThat(statusNaListagem(DESAFIO_FINALIZADO_ID)).isEqualTo("concluido_empate");
        assertThat(statusNoDetalhe(DESAFIO_FINALIZADO_ID)).isEqualTo("concluido_empate");
    }

    @Test
    void deve_avancar_todos_os_desafios_ativos_de_uma_vez() throws Exception {
        UUID segundoDesafioId = criarDesafioDePaginas();
        definirPlacar(DESAFIO_ATIVO_ID, DANIEL_ID, 120);

        registrar(DOM_CASMURRO_ID, 185);

        assertThat(placar(DESAFIO_ATIVO_ID, DANIEL_ID)).isEqualTo(160);
        assertThat(placar(segundoDesafioId, DANIEL_ID)).isEqualTo(40);
    }

    @Test
    void nao_deve_avancar_desafio_pendente() {
        int placarAntes = placar(DESAFIO_PENDENTE_ID, DANIEL_ID);

        registrar(DOM_CASMURRO_ID, 200);

        assertThat(placar(DESAFIO_PENDENTE_ID, DANIEL_ID)).isEqualTo(placarAntes);
        assertThat(statusNoBanco(DESAFIO_PENDENTE_ID)).isEqualTo("pendente");
    }

    @Test
    void nao_deve_avancar_desafio_fora_da_janela() {
        alterarDesafio(
                "inicio_em = now() + interval '1 day', fim_em = now() + interval '2 days'",
                DESAFIO_ATIVO_ID);
        definirPlacar(DESAFIO_ATIVO_ID, DANIEL_ID, 150);

        registrar(DOM_CASMURRO_ID, 200);

        assertThat(placar(DESAFIO_ATIVO_ID, DANIEL_ID)).isEqualTo(150);
        assertThat(statusNoBanco(DESAFIO_ATIVO_ID)).isEqualTo("ativo");
    }

    @Test
    void deve_listar_desafio_recem_criado_como_em_andamento() throws Exception {
        UUID desafioId = criarDesafioDePaginas();

        assertThat(statusNaListagem(desafioId)).isEqualTo("em_andamento");
        assertThat(statusNoBanco(desafioId)).isEqualTo("ativo");
    }

    @Test
    void nao_deve_avancar_placar_ao_voltar_para_pagina_ja_lida() {
        definirPlacar(DESAFIO_ATIVO_ID, DANIEL_ID, 150);

        registrar(DOM_CASMURRO_ID, 100);

        assertThat(placar(DESAFIO_ATIVO_ID, DANIEL_ID)).isEqualTo(150);
    }

    @Test
    void nao_deve_gravar_placar_quando_a_pagina_e_invalida() {
        definirPlacar(DESAFIO_ATIVO_ID, DANIEL_ID, 150);

        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", DOM_CASMURRO_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":300}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY);

        assertThat(placar(DESAFIO_ATIVO_ID, DANIEL_ID)).isEqualTo(150);
    }

    @Test
    void deve_registrar_leitura_sem_erro_quando_nao_ha_desafio_ativo() {
        alterarDesafio("status = 'finalizado'", DESAFIO_ATIVO_ID);
        definirPlacar(DESAFIO_ATIVO_ID, DANIEL_ID, 150);

        registrar(DOM_CASMURRO_ID, 200);

        assertThat(placar(DESAFIO_ATIVO_ID, DANIEL_ID)).isEqualTo(150);
    }

    private void registrar(UUID livroId, int pagina) {
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", livroId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":" + pagina + "}"))
                .hasStatusOk();
    }

    private UUID criarDesafioDePaginas() throws Exception {
        return criarDesafio(
                """
                {"oponenteId": "%s", "tipoMeta": "paginas", "meta": 150, "prazoDias": 7}
                """
                        .formatted(ANA_ID));
    }

    private UUID criarDesafioDeLivro(UUID livroId) throws Exception {
        return criarDesafio(
                """
                {"oponenteId": "%s", "tipoMeta": "livro", "livroId": "%s", "prazoDias": 7}
                """
                        .formatted(ANA_ID, livroId));
    }

    private UUID criarDesafio(String corpo) throws Exception {
        MvcTestResult resultado =
                mvc.post()
                        .uri("/api/desafios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo)
                        .exchange();

        assertThat(resultado).hasStatus(HttpStatus.CREATED);

        return UUID.fromString(lerJson(resultado).path("id").textValue());
    }

    private String statusNoDetalhe(UUID desafioId) throws Exception {
        MvcTestResult resultado = mvc.get().uri("/api/desafios/{id}", desafioId).exchange();

        assertThat(resultado).hasStatusOk();

        return lerJson(resultado).path("status").textValue();
    }

    private String statusNaListagem(UUID desafioId) throws Exception {
        MvcTestResult resultado = mvc.get().uri("/api/desafios").param("limit", "50").exchange();

        assertThat(resultado).hasStatusOk();

        for (JsonNode desafio : lerJson(resultado).path("desafios")) {
            if (desafioId.toString().equals(desafio.path("id").textValue())) {
                return desafio.path("status").textValue();
            }
        }

        throw new AssertionError("Desafio fora da listagem: " + desafioId);
    }

    private void encerrarPrazo(UUID desafioId) {
        alterarDesafio("fim_em = now() - interval '1 hour'", desafioId);
    }

    // O SQL direto passa por fora do Hibernate: o flush grava o que ele tem pendente antes, e o
    // clear descarta as entidades que ficariam desatualizadas depois.
    private void alterarDesafio(String atribuicoes, UUID desafioId) {
        entityManager.flush();
        jdbc.update("UPDATE desafio_amigo SET " + atribuicoes + " WHERE id = ?", desafioId);
        entityManager.clear();
    }

    private void definirPlacar(UUID desafioId, UUID usuarioId, int valor) {
        entityManager.flush();
        jdbc.update(
                "UPDATE progresso_desafio SET valor_atual = ?"
                        + " WHERE desafio_id = ? AND usuario_id = ?",
                valor,
                desafioId,
                usuarioId);
        entityManager.clear();
    }

    private int placar(UUID desafioId, UUID usuarioId) {
        entityManager.flush();

        return jdbc.queryForObject(
                "SELECT valor_atual FROM progresso_desafio"
                        + " WHERE desafio_id = ? AND usuario_id = ?",
                Integer.class,
                desafioId,
                usuarioId);
    }

    private String statusNoBanco(UUID desafioId) {
        entityManager.flush();

        return jdbc.queryForObject(
                "SELECT status FROM desafio_amigo WHERE id = ?", String.class, desafioId);
    }

    private JsonNode lerJson(MvcTestResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }
}
