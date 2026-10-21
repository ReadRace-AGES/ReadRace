package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.model.UsuarioId;
import com.readrace.api.service.DesafiosSistemaService;
import com.readrace.api.service.UsuarioAtual;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({TestcontainersConfiguration.class, DesafiosSistemaControllerIT.Config.class})
class DesafiosSistemaControllerIT {
    private static final UUID USUARIO = UUID.fromString("19800000-0000-0000-0000-000000000001");
    private static final UUID OUTRO = UUID.fromString("19800000-0000-0000-0000-000000000002");
    private static final Instant AGORA = Instant.parse("2026-10-18T15:00:00Z");
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvcTester mvc;
    @Autowired ObjectMapper mapper;
    @Autowired DesafiosSistemaService service;
    @Autowired EntityManager entityManager;

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        Clock relogioFixo() {
            return Clock.fixed(AGORA, ZoneOffset.UTC);
        }

        @Bean
        @Primary
        UsuarioAtual usuarioDoTeste() {
            return () -> new UsuarioId(USUARIO);
        }
    }

    @BeforeEach
    void preparar() {
        for (UUID usuario : new UUID[] {USUARIO, OUTRO}) {
            jdbc.update(
                    "INSERT INTO usuario (id, nome, nome_usuario, email) VALUES (?, ?, ?, ?)",
                    usuario,
                    "Teste 198",
                    "teste" + usuario,
                    usuario + "@example.com");
        }
    }

    @Test
    void contrato_completo_inclui_ambos_zerados_e_titulo_explicitamente_nulo() throws Exception {
        JsonNode esperado =
                mapper.readTree(
                        """
                {"mensal":{"titulo":"Desafio da leitura mensal","descricao":"Leia 2 livros este mês",
                "meta":2,"progresso":0,"unidade":"livros","diasRestantes":14,"xp":200,"cumprido":false},
                "semanal":{"titulo":null,"descricao":"Leia 70 páginas essa semana",
                "meta":70,"progresso":0,"unidade":"paginas","diasRestantes":1,"xp":100,"cumprido":false}}
                """);
        assertThat(consultar()).isEqualTo(esperado);
    }

    @Test
    void semanal_conta_45_paginas_sobre_maxima_historica_ignorando_recuos_e_repeticoes()
            throws Exception {
        UUID item = item(USUARIO, 1, false);
        registro(item, 100, "2026-10-11T23:00:00Z");
        registro(item, 20, "2026-10-12T04:00:00Z");
        registro(item, 100, "2026-10-13T12:00:00Z");
        registro(item, 120, "2026-10-14T12:00:00Z");
        registro(item, 120, "2026-10-14T12:00:00Z");
        registro(item, 80, "2026-10-15T12:00:00Z");
        registro(item, 145, "2026-10-16T12:00:00Z");
        registro(item, 200, "2026-10-18T15:00:01Z");
        var resposta = consultar().path("semanal");
        assertThat(resposta.path("progresso").intValue()).isEqualTo(45);
        assertThat(resposta.path("cumprido").booleanValue()).isFalse();
    }

    @Test
    void soma_livros_limita_resposta_e_preserva_bruto() throws Exception {
        UUID primeiro = item(USUARIO, 1, false);
        UUID segundo = item(USUARIO, 2, false);
        registro(primeiro, 50, "2026-10-13T12:00:00Z");
        registro(segundo, 40, "2026-10-14T12:00:00Z");
        var resposta = consultar().path("semanal");
        assertThat(resposta.path("progresso").intValue()).isEqualTo(70);
        assertThat(resposta.path("cumprido").booleanValue()).isTrue();
        assertThat(service.medir(USUARIO, LocalDate.of(2026, 10, 18), AGORA).semanal())
                .isEqualTo(90);
    }

    @Test
    void registros_so_anteriores_e_de_outro_usuario_nao_contam() throws Exception {
        UUID anterior = item(USUARIO, 1, false);
        registro(anterior, 80, "2026-09-01T12:00:00Z");
        UUID alheio = item(OUTRO, 2, false);
        registro(alheio, total(2), "2026-10-13T12:00:00Z");
        var resposta = consultar();
        assertThat(resposta.path("mensal").path("progresso").intValue()).isZero();
        assertThat(resposta.path("semanal").path("progresso").intValue()).isZero();
    }

    @Test
    void mensal_conta_primeira_conclusao_sem_usar_status_ou_repeticao() throws Exception {
        UUID primeiro = item(USUARIO, 1, false);
        registro(primeiro, total(1), "2026-10-01T03:00:00Z");
        registro(primeiro, total(1), "2026-10-10T12:00:00Z");
        UUID antigo = item(USUARIO, 2, false);
        registro(antigo, total(2), "2026-10-01T02:59:59Z");
        registro(antigo, total(2), "2026-10-13T12:00:00Z");
        item(USUARIO, 3, true);
        UUID futuro = item(USUARIO, 4, false);
        registro(futuro, total(4), "2026-10-18T15:00:01Z");
        var resposta = consultar().path("mensal");
        assertThat(resposta.path("progresso").intValue()).isEqualTo(1);
        assertThat(resposta.path("cumprido").booleanValue()).isFalse();
    }

    @Test
    void mensal_limita_tres_conclusoes_a_dois() throws Exception {
        for (int livro = 1; livro <= 3; livro++) {
            registro(item(USUARIO, livro, false), total(livro), "2026-10-10T12:00:00Z");
        }
        var resposta = consultar().path("mensal");
        assertThat(resposta.path("progresso").intValue()).isEqualTo(2);
        assertThat(resposta.path("cumprido").booleanValue()).isTrue();
        assertThat(service.medir(USUARIO, LocalDate.of(2026, 10, 18), AGORA).mensal()).isEqualTo(3);
    }

    @Test
    void limites_da_semana_sao_inclusivo_e_exclusivo_mesmo_ao_consultar_periodo_passado() {
        UUID item = item(USUARIO, 1, false);
        registro(item, 10, "2026-10-05T02:59:59Z");
        registro(item, 30, "2026-10-05T03:00:00Z");
        registro(item, 50, "2026-10-12T02:59:59Z");
        registro(item, 90, "2026-10-12T03:00:00Z");
        assertThat(service.medir(USUARIO, LocalDate.of(2026, 10, 7), AGORA).semanal())
                .isEqualTo(40);
    }

    @Test
    void limites_do_mes_e_do_instante_consultado_sao_respeitados() throws Exception {
        registro(item(USUARIO, 1, false), total(1), "2026-10-01T03:00:00Z");
        registro(item(USUARIO, 2, false), total(2), "2026-11-01T03:00:00Z");
        registro(item(USUARIO, 3, false), total(3), "2026-10-18T15:00:00Z");
        assertThat(consultar().path("mensal").path("progresso").intValue()).isEqualTo(2);
        var depois = Instant.parse("2026-12-01T12:00:00Z");
        assertThat(service.medir(USUARIO, LocalDate.of(2026, 10, 18), depois).mensal())
                .isEqualTo(2);
        assertThat(service.medir(USUARIO, LocalDate.of(2026, 11, 1), depois).mensal()).isEqualTo(1);
    }

    @Test
    void consultas_repetidas_nao_alteram_estado_persistido() throws Exception {
        registro(item(USUARIO, 1, false), total(1), "2026-10-13T12:00:00Z");
        var usuarioAntes = jdbc.queryForList("SELECT * FROM usuario WHERE id = ?", USUARIO);
        var registrosAntes = jdbc.queryForList("SELECT * FROM registro_leitura ORDER BY id");
        var itensAntes = jdbc.queryForList("SELECT * FROM item_biblioteca ORDER BY id");
        var desafiosAntes = jdbc.queryForList("SELECT * FROM desafio_amigo ORDER BY id");
        var progressosAntes = jdbc.queryForList("SELECT * FROM progresso_desafio ORDER BY id");
        var conquistasAntes = jdbc.queryForList("SELECT * FROM conquista_usuario ORDER BY id");
        assertThat(consultar()).isEqualTo(consultar());
        entityManager.flush();
        assertThat(jdbc.queryForList("SELECT * FROM usuario WHERE id = ?", USUARIO))
                .isEqualTo(usuarioAntes);
        assertThat(jdbc.queryForList("SELECT * FROM registro_leitura ORDER BY id"))
                .isEqualTo(registrosAntes);
        assertThat(jdbc.queryForList("SELECT * FROM item_biblioteca ORDER BY id"))
                .isEqualTo(itensAntes);
        assertThat(jdbc.queryForList("SELECT * FROM desafio_amigo ORDER BY id"))
                .isEqualTo(desafiosAntes);
        assertThat(jdbc.queryForList("SELECT * FROM progresso_desafio ORDER BY id"))
                .isEqualTo(progressosAntes);
        assertThat(jdbc.queryForList("SELECT * FROM conquista_usuario ORDER BY id"))
                .isEqualTo(conquistasAntes);
    }

    private JsonNode consultar() throws Exception {
        var resultado = mvc.get().uri("/api/desafios/sistema").exchange();
        assertThat(resultado).hasStatusOk();
        return mapper.readTree(resultado.getResponse().getContentAsString());
    }

    private UUID livro(int numero) {
        return UUID.fromString("30000000-0000-0000-0000-%012d".formatted(numero));
    }

    private int total(int numero) {
        return jdbc.queryForObject(
                "SELECT total_paginas FROM livro WHERE id = ?", Integer.class, livro(numero));
    }

    private UUID item(UUID usuario, int numero, boolean lido) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO item_biblioteca (id, usuario_id, livro_id, status_leitura, pagina_atual, pagina_maxima)
                VALUES (?, ?, ?, CAST(? AS status_leitura), ?, ?)
                """,
                id,
                usuario,
                livro(numero),
                lido ? "lido" : "lendo",
                lido ? total(numero) : 0,
                lido ? total(numero) : 0);
        return id;
    }

    private void registro(UUID item, int pagina, String instante) {
        jdbc.update(
                "INSERT INTO registro_leitura (id, item_biblioteca_id, ultima_pagina, registrado_em) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(),
                item,
                pagina,
                OffsetDateTime.parse(instante));
    }
}
