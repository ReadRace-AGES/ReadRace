package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

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
import com.readrace.api.service.UsuarioAtual;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({TestcontainersConfiguration.class, FeedSocialControllerIT.Config.class})
class FeedSocialControllerIT {
    private static final UUID EU = UUID.fromString("19700000-0000-0000-0000-000000000001");
    private static final UUID ANA = UUID.fromString("19700000-0000-0000-0000-000000000002");
    private static final UUID BRUNO = UUID.fromString("19700000-0000-0000-0000-000000000003");
    private static final UUID NAO_SIGO = UUID.fromString("19700000-0000-0000-0000-000000000004");
    // 12h em São Paulo.
    private static final Instant AGORA = Instant.parse("2026-10-18T15:00:00Z");
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvcTester mvc;
    @Autowired ObjectMapper mapper;

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
            return () -> new UsuarioId(EU);
        }
    }

    @BeforeEach
    void preparar() {
        usuario(EU, "Eu");
        usuario(ANA, "Ana Teste");
        usuario(BRUNO, "Bruno Teste");
        usuario(NAO_SIGO, "Não Sigo");
        seguir(EU, ANA);
        seguir(EU, BRUNO);
    }

    @Test
    void contrato_completo_de_um_card_de_leitura() throws Exception {
        jdbc.update(
                "UPDATE usuario SET avatar_url = ?, dias_consecutivos = 4, ultima_leitura_em = ?"
                        + " WHERE id = ?",
                "https://exemplo.com/ana.png",
                LocalDate.of(2026, 10, 18),
                ANA);
        UUID item = item(ANA, 2);
        registro(item, 30, "2026-10-18T13:00:00Z");

        JsonNode esperado =
                mapper.readTree(
                        """
                {"atividades":[{"tipo":"leitura",
                "usuario":{"id":"19700000-0000-0000-0000-000000000002","nome":"Ana Teste",
                "avatarUrl":"https://exemplo.com/ana.png","sequenciaDias":4},
                "livro":{"id":"30000000-0000-0000-0000-000000000002","titulo":"1984",
                "autor":"George Orwell","totalPaginas":328},
                "paginas":30,"xp":30,"em":"2026-10-18T10:00:00-03:00"}]}
                """);
        assertThat(consultar()).isEqualTo(esperado);
    }

    @Test
    void dois_registros_no_mesmo_dia_viram_um_unico_card_com_as_paginas_somadas() throws Exception {
        UUID item = item(ANA, 1);
        registro(item, 20, "2026-10-18T11:00:00Z");
        registro(item, 50, "2026-10-18T14:00:00Z");

        JsonNode atividades = consultar().path("atividades");
        assertThat(atividades).hasSize(1);
        assertThat(atividades.get(0).path("paginas").intValue()).isEqualTo(50);
        assertThat(atividades.get(0).path("xp").intValue()).isEqualTo(50);
        assertThat(atividades.get(0).path("em").stringValue())
                .isEqualTo("2026-10-18T11:00:00-03:00");
    }

    @Test
    void voltar_a_pagina_nao_gera_card_nem_paginas() throws Exception {
        UUID item = item(ANA, 1);
        registro(item, 80, "2026-10-16T12:00:00Z");
        registro(item, 60, "2026-10-17T12:00:00Z");

        JsonNode atividades = consultar().path("atividades");
        assertThat(atividades).hasSize(1);
        assertThat(atividades.get(0).path("paginas").intValue()).isEqualTo(80);
        assertThat(atividades.get(0).path("em").stringValue()).startsWith("2026-10-16");
    }

    @Test
    void reler_parte_da_maxima_de_um_dia_anterior_so_conta_o_que_passa_dela() throws Exception {
        UUID item = item(ANA, 1);
        registro(item, 100, "2026-10-14T12:00:00Z");
        registro(item, 20, "2026-10-15T12:00:00Z");
        registro(item, 100, "2026-10-16T12:00:00Z");
        registro(item, 90, "2026-10-17T12:00:00Z");
        registro(item, 120, "2026-10-17T13:00:00Z");

        JsonNode atividades = consultar().path("atividades");
        assertThat(atividades).hasSize(2);
        assertThat(atividades.get(0).path("paginas").intValue()).isEqualTo(20);
        assertThat(atividades.get(1).path("paginas").intValue()).isEqualTo(100);
    }

    @Test
    void o_dia_do_card_e_o_de_sao_paulo() throws Exception {
        UUID item = item(ANA, 1);
        // 23:59 e 00:00 em São Paulo: dias diferentes, apesar de ser o mesmo dia em UTC.
        registro(item, 10, "2026-10-17T02:59:00Z");
        registro(item, 25, "2026-10-17T03:00:00Z");

        JsonNode atividades = consultar().path("atividades");
        assertThat(atividades).hasSize(2);
        assertThat(atividades.get(0).path("paginas").intValue()).isEqualTo(15);
        assertThat(atividades.get(1).path("paginas").intValue()).isEqualTo(10);
    }

    @Test
    void um_card_por_pessoa_livro_e_dia_do_mais_recente_para_o_mais_antigo() throws Exception {
        UUID livroDaAna = item(ANA, 1);
        UUID outroLivroDaAna = item(ANA, 3);
        UUID livroDoBruno = item(BRUNO, 1);
        registro(livroDaAna, 10, "2026-10-15T12:00:00Z");
        registro(livroDoBruno, 40, "2026-10-17T12:00:00Z");
        registro(outroLivroDaAna, 5, "2026-10-18T12:00:00Z");
        registro(livroDaAna, 30, "2026-10-18T13:00:00Z");

        JsonNode atividades = consultar().path("atividades");
        assertThat(atividades).hasSize(4);
        assertThat(atividades.get(0).path("paginas").intValue()).isEqualTo(20);
        assertThat(atividades.get(1).path("paginas").intValue()).isEqualTo(5);
        assertThat(atividades.get(2).path("usuario").path("id").stringValue())
                .isEqualTo(BRUNO.toString());
        assertThat(atividades.get(3).path("paginas").intValue()).isEqualTo(10);
    }

    @Test
    void terminar_o_livro_gera_o_card_de_conclusao_alem_do_de_leitura() throws Exception {
        UUID item = item(ANA, 2);
        registro(item, 300, "2026-10-17T12:00:00Z");
        registro(item, 328, "2026-10-18T13:00:00Z");

        JsonNode atividades = consultar().path("atividades");
        assertThat(atividades).hasSize(3);
        JsonNode conclusao = atividades.get(0);
        assertThat(conclusao.path("tipo").stringValue()).isEqualTo("conclusao");
        assertThat(conclusao.path("paginas").intValue()).isEqualTo(328);
        assertThat(conclusao.path("xp").intValue()).isEqualTo(150);
        assertThat(conclusao.path("livro").path("totalPaginas").intValue()).isEqualTo(328);
        assertThat(conclusao.path("em").stringValue()).isEqualTo("2026-10-18T10:00:00-03:00");
        JsonNode leituraDoDia = atividades.get(1);
        assertThat(leituraDoDia.path("tipo").stringValue()).isEqualTo("leitura");
        assertThat(leituraDoDia.path("paginas").intValue()).isEqualTo(28);
        assertThat(leituraDoDia.path("xp").intValue()).isEqualTo(28);
        assertThat(leituraDoDia.path("em").stringValue()).isEqualTo(conclusao.path("em").stringValue());
        assertThat(atividades.get(2).path("paginas").intValue()).isEqualTo(300);
    }

    @Test
    void a_conclusao_e_a_hora_em_que_chegou_ao_fim_mesmo_lendo_depois_no_mesmo_dia()
            throws Exception {
        UUID item = item(ANA, 7);
        registro(item, 88, "2026-10-18T11:00:00Z");
        registro(item, 40, "2026-10-18T13:00:00Z");

        JsonNode atividades = consultar().path("atividades");
        assertThat(atividades).hasSize(2);
        assertThat(atividades.get(0).path("tipo").stringValue()).isEqualTo("leitura");
        assertThat(atividades.get(0).path("em").stringValue())
                .isEqualTo("2026-10-18T10:00:00-03:00");
        assertThat(atividades.get(1).path("tipo").stringValue()).isEqualTo("conclusao");
        assertThat(atividades.get(1).path("em").stringValue())
                .isEqualTo("2026-10-18T08:00:00-03:00");
    }

    @Test
    void chegar_ao_fim_de_novo_outro_dia_nao_gera_segunda_conclusao() throws Exception {
        UUID item = item(ANA, 7);
        registro(item, 88, "2026-10-14T12:00:00Z");
        registro(item, 30, "2026-10-15T12:00:00Z");
        registro(item, 88, "2026-10-16T12:00:00Z");

        JsonNode atividades = consultar().path("atividades");
        assertThat(atividades).hasSize(2);
        assertThat(atividades.get(0).path("tipo").stringValue()).isEqualTo("conclusao");
        assertThat(atividades.get(0).path("em").stringValue()).startsWith("2026-10-14");
        assertThat(atividades.get(1).path("tipo").stringValue()).isEqualTo("leitura");
        assertThat(atividades.get(1).path("em").stringValue()).startsWith("2026-10-14");
    }

    @Test
    void conclusao_de_antes_dos_30_dias_nao_aparece() throws Exception {
        UUID item = item(ANA, 7);
        registro(item, 88, "2026-09-10T12:00:00Z");
        registro(item, 88, "2026-10-17T12:00:00Z");

        assertThat(consultar().path("atividades")).isEmpty();
    }

    @Test
    void minha_leitura_e_a_de_quem_nao_sigo_nao_aparecem() throws Exception {
        registro(item(EU, 7), 88, "2026-10-18T12:00:00Z");
        registro(item(NAO_SIGO, 7), 88, "2026-10-18T12:00:00Z");
        // Quem me segue sem eu seguir de volta também fica de fora.
        seguir(NAO_SIGO, EU);

        assertThat(consultar().path("atividades")).isEmpty();
    }

    @Test
    void quem_nao_segue_ninguem_recebe_lista_vazia() throws Exception {
        jdbc.update("DELETE FROM seguir WHERE seguidor_id = ?", EU);
        registro(item(ANA, 1), 50, "2026-10-18T12:00:00Z");

        assertThat(consultar().path("atividades")).isEmpty();
    }

    @Test
    void usuario_excluido_nao_aparece() throws Exception {
        registro(item(ANA, 1), 50, "2026-10-18T12:00:00Z");
        jdbc.update("UPDATE usuario SET excluido_em = now() WHERE id = ?", ANA);

        assertThat(consultar().path("atividades")).isEmpty();
    }

    @Test
    void so_entram_os_ultimos_30_dias_e_nada_do_futuro() throws Exception {
        UUID item = item(ANA, 6);
        registro(item, 10, "2026-09-17T12:00:00Z");
        registro(item, 30, "2026-09-18T15:00:00Z");
        registro(item, 90, "2026-10-18T15:00:01Z");
        registro(item(ANA, 7), 40, "2026-09-18T14:59:59Z");

        JsonNode atividades = consultar().path("atividades");
        assertThat(atividades).hasSize(1);
        assertThat(atividades.get(0).path("paginas").intValue()).isEqualTo(20);
    }

    @Test
    void no_maximo_50_cards() throws Exception {
        // Um registro por dia, uma página a mais que o anterior: 29 cards de cada um, 58 no total.
        Instant ontem = AGORA.minusSeconds(86_400L);
        for (UUID usuario : new UUID[] {ANA, BRUNO}) {
            UUID item = item(usuario, 16);
            for (int dias = 0; dias < 29; dias++) {
                registro(item, 29 - dias, ontem.minusSeconds(dias * 86_400L).toString());
            }
        }

        assertThat(consultar().path("atividades")).hasSize(50);
    }

    private JsonNode consultar() throws Exception {
        var resultado = mvc.get().uri("/api/feed/social").exchange();
        assertThat(resultado).hasStatusOk();
        return mapper.readTree(resultado.getResponse().getContentAsString());
    }

    private void usuario(UUID id, String nome) {
        jdbc.update(
                "INSERT INTO usuario (id, nome, nome_usuario, email) VALUES (?, ?, ?, ?)",
                id,
                nome,
                "teste" + id,
                id + "@example.com");
    }

    private void seguir(UUID seguidor, UUID seguido) {
        jdbc.update(
                "INSERT INTO seguir (id, seguidor_id, seguido_id) VALUES (?, ?, ?)",
                UUID.randomUUID(),
                seguidor,
                seguido);
    }

    private UUID item(UUID usuario, int livro) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO item_biblioteca (id, usuario_id, livro_id, status_leitura)
                VALUES (?, ?, ?, 'lendo')
                """,
                id,
                usuario,
                UUID.fromString("30000000-0000-0000-0000-%012d".formatted(livro)));
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
