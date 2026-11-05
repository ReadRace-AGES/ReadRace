package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.json.AbstractJsonContentAssert;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.model.CurvaDeNivel;
import com.readrace.api.model.SequenciaDeLeitura;

/** Os cenários da #203: a resposta do registro traz tudo o que o aviso de recompensa mostra. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Recompensa na resposta do registro de leitura")
class RecompensaDaLeituraIT {

    // Seed: o Daniel (usuário atual) parou Dom Casmurro (256 páginas) na página 145, não tem Cem
    // Anos de Solidão na biblioteca e já tem a conquista Mil Páginas. Cada teste ajusta o que
    // precisa e tudo é desfeito por rollback.
    private static final UUID DANIEL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DOM_CASMURRO_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID LIVRO_1984_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID ORGULHO_E_PRECONCEITO_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID ENSAIO_SOBRE_A_CEGUEIRA_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000012");
    // Fora da biblioteca do Daniel: entra já na página que o teste precisar.
    private static final UUID CEM_ANOS_DE_SOLIDAO_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000013");
    private static final int TOTAL_DOM_CASMURRO = 256;

    @Autowired private MockMvcTester mvc;

    @Autowired private JdbcTemplate jdbc;

    @Autowired private EntityManager entityManager;

    private final LocalDate hoje = SequenciaDeLeitura.hoje();

    @Test
    void deve_trazer_o_xp_das_paginas_e_a_sequencia_do_dia() {
        adicionarNaPagina(CEM_ANOS_DE_SOLIDAO_ID, 40);
        definirSequencia(3, hoje.minusDays(1));

        registrar(CEM_ANOS_DE_SOLIDAO_ID, 82)
                .extractingPath("$")
                .asMap()
                .containsEntry("xpPaginas", 42)
                .containsEntry("sequenciaDias", 4)
                .containsEntry("xpConclusao", 0)
                .containsEntry("subiuDeNivel", false);
    }

    @Test
    void deve_trazer_o_xp_das_paginas_e_o_bonus_de_conclusao_juntos() {
        colocarNaPagina(DOM_CASMURRO_ID, TOTAL_DOM_CASMURRO - 20);

        registrar(DOM_CASMURRO_ID, TOTAL_DOM_CASMURRO)
                .extractingPath("$")
                .asMap()
                .containsEntry("xpPaginas", 20)
                .containsEntry("xpConclusao", 150)
                .containsEntry("concluido", true);
    }

    @Test
    void deve_avisar_que_subiu_de_nivel() {
        int nivel = nivel();
        // A 10 XP do próximo nível: 15 páginas novas passam da meta.
        definirXp(CurvaDeNivel.xpParaChegarNoNivel(nivel + 1) - 10);

        registrar(DOM_CASMURRO_ID, 160)
                .extractingPath("$")
                .asMap()
                .containsEntry("xpPaginas", 15)
                .containsEntry("subiuDeNivel", true)
                .containsEntry("nivel", nivel + 1);
    }

    @Test
    void deve_trazer_a_conquista_de_paginas_lidas_quando_cruza_a_meta() {
        apagarConquista("Mil Páginas");
        manterSoLivros(ORGULHO_E_PRECONCEITO_ID, ENSAIO_SOBRE_A_CEGUEIRA_ID, LIVRO_1984_ID);
        // Soma de 995 páginas: faltam 5 para as 1000 da conquista.
        colocarNaPagina(LIVRO_1984_ID, 211);

        registrar(LIVRO_1984_ID, 216)
                .extractingPath("$.conquistasDesbloqueadas[*].nome")
                .asArray()
                .containsExactly("Mil Páginas");
    }

    @Test
    void nao_deve_trazer_recompensa_ao_voltar_para_uma_pagina_menor() {
        adicionarNaPagina(CEM_ANOS_DE_SOLIDAO_ID, 82);
        definirSequencia(4, hoje);

        registrar(CEM_ANOS_DE_SOLIDAO_ID, 60)
                .extractingPath("$")
                .asMap()
                .containsEntry("xpPaginas", 0)
                .containsEntry("xpConclusao", 0)
                .containsEntry("subiuDeNivel", false)
                .containsEntry("conquistasDesbloqueadas", Collections.emptyList());
    }

    private AbstractJsonContentAssert<?> registrar(UUID livroId, int pagina) {
        return assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", livroId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":" + pagina + "}"))
                .hasStatusOk()
                .bodyJson();
    }

    // O SQL direto passa por fora do Hibernate: o flush grava o que ele tem pendente antes, e o
    // clear descarta as entidades que ficariam desatualizadas depois.
    private void executar(String sql, Object... parametros) {
        entityManager.flush();
        jdbc.update(sql, parametros);
        entityManager.clear();
    }

    private void colocarNaPagina(UUID livroId, int pagina) {
        executar(
                "UPDATE item_biblioteca SET pagina_atual = ?, pagina_maxima = ?,"
                        + " status_leitura = 'lendo' WHERE usuario_id = ? AND livro_id = ?",
                pagina,
                pagina,
                DANIEL_ID,
                livroId);
    }

    // A página máxima não pode diminuir (trigger do banco): para parar abaixo do seed, o livro
    // entra na biblioteca já na página certa.
    private void adicionarNaPagina(UUID livroId, int pagina) {
        executar(
                "INSERT INTO item_biblioteca"
                        + " (id, usuario_id, livro_id, status_leitura, pagina_atual, pagina_maxima)"
                        + " VALUES (?, ?, ?, 'lendo', ?, ?)",
                UUID.randomUUID(),
                DANIEL_ID,
                livroId,
                pagina,
                pagina);
    }

    private void definirSequencia(int dias, LocalDate ultimaLeitura) {
        executar(
                "UPDATE usuario SET dias_consecutivos = ?, ultima_leitura_em = ? WHERE id = ?",
                dias,
                ultimaLeitura,
                DANIEL_ID);
    }

    private void definirXp(int xpTotal) {
        executar(
                "UPDATE usuario SET xp_total = ?, nivel = ? WHERE id = ?",
                xpTotal,
                CurvaDeNivel.nivelDoXp(xpTotal),
                DANIEL_ID);
    }

    private void apagarConquista(String nome) {
        executar(
                "DELETE FROM conquista_usuario WHERE usuario_id = ?"
                        + " AND conquista_id = (SELECT id FROM conquista WHERE nome = ?)",
                DANIEL_ID,
                nome);
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

    private int nivel() {
        entityManager.flush();
        return jdbc.queryForObject(
                "SELECT nivel FROM usuario WHERE id = ?", Integer.class, DANIEL_ID);
    }
}
