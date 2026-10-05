package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Collections;
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
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Desbloqueio automático de conquistas")
class ConquistaAutomaticaIT {

    // Seed: o Daniel (usuário atual) tem 5 livros lidos, 1855 páginas máximas somadas, 2 desafios
    // finalizados e 5 das 6 conquistas. A Ana não tem conquista nem desafio finalizado.
    private static final UUID DANIEL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ANA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID DESAFIO_ATIVO_ID =
            UUID.fromString("90000000-0000-0000-0000-000000000001");

    private static final UUID DOM_CASMURRO_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID LIVRO_1984_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID ORGULHO_E_PRECONCEITO_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID A_HORA_DA_ESTRELA_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000007");
    private static final UUID O_PEQUENO_PRINCIPE_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000009");
    private static final UUID ENSAIO_SOBRE_A_CEGUEIRA_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000012");

    private static final String PRIMEIROS_PASSOS = "Primeiros Passos";
    private static final String LEITOR_DEDICADO = "Leitor Dedicado";
    private static final String MIL_PAGINAS = "Mil Páginas";
    private static final String EM_BOA_COMPANHIA = "Em Boa Companhia";
    private static final String COMPETIDOR = "Competidor";

    @Autowired private MockMvcTester mvc;

    @Autowired private JdbcTemplate jdbc;

    @Autowired private EntityManager entityManager;

    @Test
    void nao_deve_duplicar_conquista_nem_pagar_xp_para_quem_ja_tem_as_do_seed() {
        int xpAntes = xp(DANIEL_ID);

        registrar(DOM_CASMURRO_ID, 150);

        assertThat(conquistas(DANIEL_ID)).hasSize(5);
        assertThat(linhasDeConquista(DANIEL_ID)).isEqualTo(5);
        // Nenhuma recompensa de conquista é paga; só as 5 páginas novas (145 -> 150) rendem XP.
        assertThat(xp(DANIEL_ID)).isEqualTo(xpAntes + 5);
    }

    @Test
    void deve_desbloquear_de_uma_vez_o_que_ja_foi_alcancado_na_primeira_avaliacao() {
        apagarConquistas(DANIEL_ID);
        marcarComoLendo(A_HORA_DA_ESTRELA_ID, O_PEQUENO_PRINCIPE_ID);
        int xpAntes = xp(DANIEL_ID);

        // A resposta já conta as recompensas: 2450 + 1 + 425 = 2876 passa dos 2762 que fecham o
        // nível 7, então o nível sobe e o XP dentro do nível 8 é o que sobrou.
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", LIVRO_1984_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":211}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("xpDoUsuario", 2876)
                .containsEntry("nivel", 8)
                .containsEntry("subiuDeNivel", true)
                .containsEntry("xpNoNivel", 114)
                .containsEntry("xpDoNivel", 1018);

        assertThat(conquistas(DANIEL_ID))
                .containsExactlyInAnyOrder(
                        PRIMEIROS_PASSOS, MIL_PAGINAS, COMPETIDOR, EM_BOA_COMPANHIA);
        // 1 de XP da página nova (210 -> 211) mais as recompensas das 4 conquistas.
        assertThat(xp(DANIEL_ID)).isEqualTo(xpAntes + 1 + 50 + 200 + 100 + 75);
        assertThat(nivel(DANIEL_ID)).isEqualTo(8);
        assertThat(
                        mvc.get()
                                .uri("/api/usuarios/{usuarioId}/perfil", DANIEL_ID)
                                .accept(MediaType.APPLICATION_JSON))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.estatisticas.conquistas")
                .isEqualTo(4);
    }

    @Test
    void deve_desbloquear_mil_paginas_quando_a_soma_cruza_a_meta() {
        apagarConquista(DANIEL_ID, MIL_PAGINAS);
        manterSoLivros(ORGULHO_E_PRECONCEITO_ID, ENSAIO_SOBRE_A_CEGUEIRA_ID, LIVRO_1984_ID);
        int xpAntes = xp(DANIEL_ID);
        assertThat(somaDePaginas(DANIEL_ID)).isEqualTo(994);

        registrar(LIVRO_1984_ID, 260);

        assertThat(conquistas(DANIEL_ID)).contains(MIL_PAGINAS);
        assertThat(obtidaEm(DANIEL_ID, MIL_PAGINAS)).isAfter(OffsetDateTime.now().minusMinutes(5));
        // 50 de XP das páginas novas (210 -> 260) mais 200 da recompensa de Mil Páginas.
        assertThat(xp(DANIEL_ID)).isEqualTo(xpAntes + 50 + 200);
    }

    @Test
    void nao_deve_pagar_o_xp_de_recompensa_duas_vezes() {
        apagarConquista(DANIEL_ID, MIL_PAGINAS);
        registrar(LIVRO_1984_ID, 211);
        int xpDepoisDoDesbloqueio = xp(DANIEL_ID);

        registrar(LIVRO_1984_ID, 212);

        // A recompensa de 200 não se repete; só a página nova (211 -> 212) rende 1 de XP.
        assertThat(xp(DANIEL_ID)).isEqualTo(xpDepoisDoDesbloqueio + 1);
        assertThat(linhasDeConquista(DANIEL_ID)).isEqualTo(5);
    }

    @Test
    void deve_desbloquear_leitor_dedicado_ao_concluir_o_quinto_livro() {
        apagarConquista(DANIEL_ID, LEITOR_DEDICADO);
        marcarComoLendo(O_PEQUENO_PRINCIPE_ID);
        int xpAntes = xp(DANIEL_ID);

        registrar(DOM_CASMURRO_ID, 256);

        assertThat(conquistas(DANIEL_ID)).contains(PRIMEIROS_PASSOS, LEITOR_DEDICADO);
        assertThat(linhasDeConquista(DANIEL_ID)).isEqualTo(5);
        // 111 de XP das páginas novas (145 -> 256), 150 de bônus de conclusão do livro e 150 da
        // recompensa de Leitor Dedicado.
        assertThat(xp(DANIEL_ID)).isEqualTo(xpAntes + 111 + 150 + 150);
    }

    @Test
    void nao_deve_gravar_conquista_nem_xp_quando_a_pagina_e_invalida() {
        apagarConquistas(DANIEL_ID);
        int xpAntes = xp(DANIEL_ID);

        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", DOM_CASMURRO_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":300}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY);

        assertThat(linhasDeConquista(DANIEL_ID)).isZero();
        assertThat(xp(DANIEL_ID)).isEqualTo(xpAntes);
    }

    @Test
    void deve_desbloquear_competidor_para_quem_perde_o_desafio() {
        encerrarPrazo(DESAFIO_ATIVO_ID);
        definirPlacar(DESAFIO_ATIVO_ID, DANIEL_ID, 200);
        definirPlacar(DESAFIO_ATIVO_ID, ANA_ID, 10);

        registrar(DOM_CASMURRO_ID, 150);

        assertThat(statusDoDesafio(DESAFIO_ATIVO_ID)).isEqualTo("finalizado");
        assertThat(conquistas(ANA_ID)).contains(COMPETIDOR);
    }

    @Test
    void deve_avaliar_os_participantes_quando_o_desafio_finaliza_ao_ser_consultado() {
        encerrarPrazo(DESAFIO_ATIVO_ID);
        int xpAntes = xp(ANA_ID);

        assertThat(mvc.get().uri("/api/desafios/{id}", DESAFIO_ATIVO_ID)).hasStatusOk();

        assertThat(conquistas(ANA_ID)).contains(COMPETIDOR);
        assertThat(xp(ANA_ID)).isGreaterThanOrEqualTo(xpAntes + 100);
    }

    @Test
    void deve_avaliar_o_criterio_de_clubes() {
        apagarConquista(DANIEL_ID, EM_BOA_COMPANHIA);

        registrar(DOM_CASMURRO_ID, 150);

        assertThat(conquistas(DANIEL_ID)).contains(EM_BOA_COMPANHIA);
    }

    private void registrar(UUID livroId, int pagina) {
        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", livroId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":" + pagina + "}"))
                .hasStatusOk();
    }

    // O SQL direto passa por fora do Hibernate: o flush grava o que ele tem pendente antes, e o
    // clear descarta as entidades que ficariam desatualizadas depois.
    private void executar(String sql, Object... parametros) {
        entityManager.flush();
        jdbc.update(sql, parametros);
        entityManager.clear();
    }

    private void apagarConquistas(UUID usuarioId) {
        executar("DELETE FROM conquista_usuario WHERE usuario_id = ?", usuarioId);
    }

    private void apagarConquista(UUID usuarioId, String nome) {
        executar(
                "DELETE FROM conquista_usuario WHERE usuario_id = ?"
                        + " AND conquista_id = (SELECT id FROM conquista WHERE nome = ?)",
                usuarioId,
                nome);
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
        String manter = String.join(",", Collections.nCopies(livroIds.length, "?"));
        Object[] parametros = new Object[livroIds.length + 1];
        parametros[0] = DANIEL_ID;
        System.arraycopy(livroIds, 0, parametros, 1, livroIds.length);
        String outrosItens =
                "SELECT id FROM item_biblioteca WHERE usuario_id = ? AND livro_id NOT IN ("
                        + manter
                        + ")";
        executar(
                "DELETE FROM registro_leitura WHERE item_biblioteca_id IN (" + outrosItens + ")",
                parametros);
        executar("DELETE FROM item_biblioteca WHERE id IN (" + outrosItens + ")", parametros);
    }

    private void encerrarPrazo(UUID desafioId) {
        executar(
                "UPDATE desafio_amigo SET fim_em = now() - interval '1 hour' WHERE id = ?",
                desafioId);
    }

    private void definirPlacar(UUID desafioId, UUID usuarioId, int valor) {
        executar(
                "UPDATE progresso_desafio SET valor_atual = ?"
                        + " WHERE desafio_id = ? AND usuario_id = ?",
                valor,
                desafioId,
                usuarioId);
    }

    private List<String> conquistas(UUID usuarioId) {
        entityManager.flush();
        return jdbc.queryForList(
                "SELECT c.nome FROM conquista_usuario cu JOIN conquista c ON c.id = cu.conquista_id"
                        + " WHERE cu.usuario_id = ?",
                String.class,
                usuarioId);
    }

    private int linhasDeConquista(UUID usuarioId) {
        entityManager.flush();
        return jdbc.queryForObject(
                "SELECT count(*) FROM conquista_usuario WHERE usuario_id = ?",
                Integer.class,
                usuarioId);
    }

    private OffsetDateTime obtidaEm(UUID usuarioId, String nome) {
        entityManager.flush();
        return jdbc.queryForObject(
                "SELECT cu.obtida_em FROM conquista_usuario cu"
                        + " JOIN conquista c ON c.id = cu.conquista_id"
                        + " WHERE cu.usuario_id = ? AND c.nome = ?",
                OffsetDateTime.class,
                usuarioId,
                nome);
    }

    private int xp(UUID usuarioId) {
        entityManager.flush();
        return jdbc.queryForObject(
                "SELECT xp_total FROM usuario WHERE id = ?", Integer.class, usuarioId);
    }

    private int nivel(UUID usuarioId) {
        entityManager.flush();
        return jdbc.queryForObject(
                "SELECT nivel FROM usuario WHERE id = ?", Integer.class, usuarioId);
    }

    private int somaDePaginas(UUID usuarioId) {
        entityManager.flush();
        return jdbc.queryForObject(
                "SELECT coalesce(sum(pagina_maxima), 0) FROM item_biblioteca WHERE usuario_id = ?",
                Integer.class,
                usuarioId);
    }

    private String statusDoDesafio(UUID desafioId) {
        entityManager.flush();
        return jdbc.queryForObject(
                "SELECT status::text FROM desafio_amigo WHERE id = ?", String.class, desafioId);
    }
}
