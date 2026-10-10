package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.EntityManager;

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
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.model.SequenciaDeLeitura;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Sequência de dias com leitura")
class SequenciaDeLeituraIT {

    // Seed: o Daniel (usuário atual) parou Dom Casmurro na página 145 de 256. O "ontem" do seed
    // vem do CURRENT_DATE do banco, então cada teste define a sequência e a data de que precisa.
    // Tudo é desfeito por rollback.
    private static final UUID DANIEL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DOM_CASMURRO_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID LIVRO_FORA_DA_BIBLIOTECA_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000013");
    private static final UUID CLUBE_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");

    @Autowired private MockMvcTester mvc;

    @Autowired private JdbcTemplate jdbc;

    @Autowired private EntityManager entityManager;

    private final LocalDate hoje = SequenciaDeLeitura.hoje();

    @Test
    void deve_somar_um_dia_quando_a_ultima_leitura_foi_ontem() {
        definirSequencia(12, hoje.minusDays(1));

        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", DOM_CASMURRO_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":200}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.sequenciaDias")
                .isEqualTo(13);

        assertThat(sequenciaNoBanco()).isEqualTo(13);
        assertThat(ultimaLeituraNoBanco()).isEqualTo(hoje);
    }

    @Test
    void nao_deve_mudar_a_sequencia_numa_segunda_leitura_no_mesmo_dia() {
        definirSequencia(12, hoje.minusDays(1));

        registrar(DOM_CASMURRO_ID, 200);
        registrar(LIVRO_FORA_DA_BIBLIOTECA_ID, 30);

        assertThat(sequenciaNoBanco()).isEqualTo(13);
    }

    @ParameterizedTest(name = "última leitura há {0} dias")
    @ValueSource(ints = {4, 7})
    void deve_somar_um_dia_depois_de_uma_pausa_de_ate_sete_dias(int diasSemLer) {
        definirSequencia(12, hoje.minusDays(diasSemLer));

        registrar(DOM_CASMURRO_ID, 200);

        assertThat(sequenciaNoBanco()).isEqualTo(13);
    }

    @Test
    void deve_voltar_para_um_depois_de_oito_dias_sem_leitura() {
        definirSequencia(12, hoje.minusDays(8));

        registrar(DOM_CASMURRO_ID, 200);

        assertThat(sequenciaNoBanco()).isEqualTo(1);
        assertThat(ultimaLeituraNoBanco()).isEqualTo(hoje);
    }

    @Test
    void deve_comecar_em_um_no_primeiro_registro() {
        definirSequencia(0, null);

        registrar(DOM_CASMURRO_ID, 200);

        assertThat(sequenciaNoBanco()).isEqualTo(1);
        assertThat(ultimaLeituraNoBanco()).isEqualTo(hoje);
    }

    @Test
    void deve_exibir_zero_no_perfil_depois_de_oito_dias_sem_leitura_sem_corrigir_a_coluna() {
        definirSequencia(12, hoje.minusDays(8));

        assertThat(mvc.get().uri("/api/usuarios/{usuarioId}/perfil", DANIEL_ID))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.estatisticas.sequenciaDias")
                .isEqualTo(0);
        assertThat(mvc.get().uri("/api/me/perfil"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.estatisticas.sequenciaDias")
                .isEqualTo(0);

        assertThat(sequenciaNoBanco()).isEqualTo(12);
    }

    @Test
    void deve_exibir_a_coluna_no_perfil_com_sete_dias_sem_leitura() {
        definirSequencia(12, hoje.minusDays(7));

        assertThat(mvc.get().uri("/api/usuarios/{usuarioId}/perfil", DANIEL_ID))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.estatisticas.sequenciaDias")
                .isEqualTo(12);
    }

    @Test
    void deve_aplicar_a_mesma_regra_no_feed_no_forum_e_no_detalhe_do_livro() {
        entityManager.flush();
        jdbc.update("UPDATE usuario SET ultima_leitura_em = ?", hoje.minusDays(8));
        entityManager.clear();

        assertThat(mvc.get().uri("/api/feed/comunidades"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.usuario.sequenciaDias")
                .isEqualTo(0);
        assertThat(mvc.get().uri("/api/clubes/{clubeId}/posts", CLUBE_ID))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.posts[*].autor.sequenciaDias")
                .asArray()
                .isNotEmpty()
                .containsOnly(0);
        assertThat(mvc.get().uri("/api/livros/{livroId}", DOM_CASMURRO_ID))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.posts[*].autor.sequenciaDias")
                .asArray()
                .isNotEmpty()
                .containsOnly(0);
    }

    @Test
    void deve_acender_a_chama_do_feed_depois_de_registrar_a_leitura_de_hoje() {
        definirSequencia(1, hoje.minusDays(1));

        assertThat(mvc.get().uri("/api/feed/comunidades"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.usuario.leuHoje")
                .isEqualTo(false);

        registrar(DOM_CASMURRO_ID, 200);

        assertThat(mvc.get().uri("/api/feed/comunidades"))
                .hasStatusOk()
                .bodyJson()
                .satisfies(
                        json -> {
                            assertThat(json).extractingPath("$.usuario.leuHoje").isEqualTo(true);
                            assertThat(json).extractingPath("$.usuario.diasAtePerder").isNull();
                        });
    }

    @Test
    void deve_avisar_no_feed_que_a_sequencia_acaba_amanha_com_sete_dias_sem_registro() {
        definirSequencia(1, hoje.minusDays(7));

        assertThat(mvc.get().uri("/api/feed/comunidades"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.usuario.diasAtePerder")
                .isEqualTo(1);
    }

    @Test
    void nao_deve_tocar_na_sequencia_quando_a_pagina_e_invalida() {
        definirSequencia(12, hoje.minusDays(1));

        assertThat(
                        mvc.post()
                                .uri("/api/livros/{livroId}/progresso", DOM_CASMURRO_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"pagina\":300}"))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY);

        assertThat(sequenciaNoBanco()).isEqualTo(12);
        assertThat(ultimaLeituraNoBanco()).isEqualTo(hoje.minusDays(1));
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
    private void definirSequencia(int dias, LocalDate ultimaLeitura) {
        entityManager.flush();
        jdbc.update(
                "UPDATE usuario SET dias_consecutivos = ?, ultima_leitura_em = ? WHERE id = ?",
                dias,
                ultimaLeitura,
                DANIEL_ID);
        entityManager.clear();
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
}
