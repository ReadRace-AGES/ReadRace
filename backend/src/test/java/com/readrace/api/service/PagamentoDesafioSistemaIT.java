package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.dto.request.RegistrarProgressoRequest;
import com.readrace.api.dto.response.ProgressoLeituraResponse;
import com.readrace.api.model.CurvaDeNivel;
import com.readrace.api.model.DesafioSistema;
import com.readrace.api.model.UsuarioId;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({TestcontainersConfiguration.class, PagamentoDesafioSistemaIT.Config.class})
class PagamentoDesafioSistemaIT {
    private static final UUID USUARIO = UUID.fromString("20500000-0000-0000-0000-000000000001");
    private static final Instant AGORA = Instant.parse("2026-10-24T15:00:00Z");
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired RegistrarProgressoService leitura;
    @Autowired DesafiosSistemaService desafios;
    @Autowired MockMvcTester mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean Clock clock;

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        UsuarioAtual usuarioDoTeste() {
            return () -> new UsuarioId(USUARIO);
        }
    }

    @BeforeEach
    void preparar_usuario_isolado_sem_desafios_amigos() {
        when(clock.instant()).thenReturn(AGORA);
        jdbc.update(
                "INSERT INTO usuario (id, nome, nome_usuario, email) VALUES (?, ?, ?, ?)",
                USUARIO,
                "Teste 205",
                "teste205",
                "teste205@example.com");
        // Isola as recompensas do sistema das recompensas de conquistas neste conjunto de testes.
        jdbc.update(
                "INSERT INTO conquista_usuario (id, conquista_id, usuario_id) SELECT gen_random_uuid(), id, ? FROM conquista",
                USUARIO);
    }

    @Test
    void semanal_60_para_75_paga_sem_amigos_sobe_nivel_e_nao_repete_no_mesmo_instante() {
        UUID item = item(1, 60);
        registro(item, 60, null, AGORA);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM desafio_amigo WHERE criador_id = ? OR oponente_id = ?",
                                Long.class,
                                USUARIO,
                                USUARIO))
                .isZero();
        assertThat(medir().semanal()).isEqualTo(60);

        var resposta = registrar(1, 75);
        assertThat(resposta.xpPaginas()).isEqualTo(15);
        assertThat(resposta.xpConclusao()).isZero();
        assertThat(resposta.xpTotal()).isEqualTo(15);
        assertThat(resposta.xpDoUsuario()).isEqualTo(115);
        assertThat(resposta.nivel()).isEqualTo(2);
        assertThat(resposta.subiuDeNivel()).isTrue();
        assertThat(resposta.xpNoNivel()).isEqualTo(CurvaDeNivel.xpNoNivel(115, 2));
        assertThat(resposta.xpDoNivel()).isEqualTo(CurvaDeNivel.xpDoNivel(2));
        assertThat(medir().semanal()).isEqualTo(75);
        assertThat(pagamentos("SEMANAL")).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT xp_pago FROM pagamento_desafio_sistema WHERE usuario_id = ?",
                                Integer.class,
                                USUARIO))
                .isEqualTo(100);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT inicio_periodo FROM pagamento_desafio_sistema WHERE usuario_id = ?",
                                LocalDate.class,
                                USUARIO))
                .isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(
                        jdbc.queryForObject(
                                        "SELECT pago_em FROM pagamento_desafio_sistema WHERE usuario_id = ?",
                                        OffsetDateTime.class,
                                        USUARIO)
                                .toInstant())
                .isEqualTo(AGORA);

        assertThat(registrar(1, 80).xpDoUsuario()).isEqualTo(120);
        assertThat(medir().semanal()).isEqualTo(80);
        assertThat(pagamentos("SEMANAL")).isEqualTo(1);
        assertThat(xpPersistido()).isEqualTo(120);
        assertThat(
                        jdbc.queryForList(
                                "SELECT paginas_novas FROM registro_leitura WHERE item_biblioteca_id = ? AND paginas_novas IS NOT NULL ORDER BY paginas_novas",
                                Integer.class,
                                item))
                .containsExactly(5, 15);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(DISTINCT registrado_em) FROM registro_leitura WHERE item_biblioteca_id = ?",
                                Long.class,
                                item))
                .isEqualTo(1);
    }

    @Test
    void segundo_livro_elegivel_paga_mensal() {
        UUID primeiro = item(1, total(1));
        registro(primeiro, total(1), null, AGORA.minusSeconds(8 * 86400));
        UUID segundo = item(2, total(2) - 1);
        registro(segundo, total(2) - 1, null, AGORA.minusSeconds(86400));
        assertThat(medir().mensal()).isEqualTo(1);
        // Já estava acima do semanal antes da operação: somente o mensal cruza sua meta.
        assertThat(medir().semanal()).isGreaterThanOrEqualTo(70);

        var resposta = registrar(2, total(2));
        assertThat(resposta.xpTotal()).isEqualTo(151);
        assertThat(resposta.xpDoUsuario()).isEqualTo(351);
        assertThat(xpPersistido()).isEqualTo(351);
        assertThat(pagamentos("MENSAL")).isEqualTo(1);
        assertThat(pagamentos("SEMANAL")).isZero();
        assertThat(medir().mensal()).isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT xp_pago FROM pagamento_desafio_sistema WHERE usuario_id = ?",
                                Integer.class,
                                USUARIO))
                .isEqualTo(200);
    }

    @Test
    void releitura_de_livro_lido_do_seed_sem_historico_nao_cria_progresso() {
        // Copia somente o estado do item lido do seed para o usuário isolado, sem alterar o seed.
        UUID item = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO item_biblioteca
                    (id, usuario_id, livro_id, status_leitura, pagina_atual, pagina_maxima)
                SELECT ?, ?, livro_id, status_leitura, pagina_atual, pagina_maxima
                FROM item_biblioteca WHERE id = '40000000-0000-0000-0000-000000000003'
                """,
                item,
                USUARIO);
        assertThat(registrar(3, total(3)).xpTotal()).isZero();
        assertThat(registrar(3, total(3) - 10).xpTotal()).isZero();
        assertThat(registrar(3, total(3)).xpTotal()).isZero();
        assertThat(medir()).isEqualTo(new DesafiosSistemaService.Medicao(0, 0));
        assertThat(pagamentos("MENSAL")).isZero();
        assertThat(pagamentos("SEMANAL")).isZero();
        assertThat(xpPersistido()).isZero();
        assertThat(
                        jdbc.queryForList(
                                "SELECT paginas_novas FROM registro_leitura WHERE item_biblioteca_id = ?",
                                Integer.class,
                                item))
                .containsExactly(0, 0, 0);
    }

    @Test
    void semanal_ja_na_meta_nao_recebe_retroativo() {
        UUID item = item(1, 75);
        registro(item, 75, null, AGORA.minusSeconds(3600));
        assertThat(registrar(1, 80).xpDoUsuario()).isEqualTo(5);
        assertThat(pagamentos("SEMANAL")).isZero();
        assertThat(xpPersistido()).isEqualTo(5);
    }

    @Test
    void empates_historicos_nao_duplicam_paginas_nem_conclusoes() {
        UUID item = item(1, total(1));
        registro(item, 20, null, AGORA);
        registro(item, total(1), null, AGORA);
        registro(item, total(1), null, AGORA);
        registro(item, 10, null, AGORA);
        assertThat(medir()).isEqualTo(new DesafiosSistemaService.Medicao(1, total(1)));
    }

    @Test
    void empate_misto_nao_duplica_intervalo_explicito_no_legado() {
        UUID item = item(1, 75);
        registro(item, 75, null, AGORA);
        registro(item, 75, 15, AGORA);
        assertThat(medir().semanal()).isEqualTo(75);
    }

    @Test
    void semanal_paga_exatamente_na_meta_e_novamente_em_outra_semana() {
        assertThat(registrar(1, 70).xpDoUsuario()).isEqualTo(170);
        assertThat(medir().semanal()).isEqualTo(70);
        when(clock.instant()).thenReturn(Instant.parse("2026-10-26T03:00:00Z"));
        assertThat(medir().semanal()).isZero();
        assertThat(registrar(1, 140).xpDoUsuario()).isEqualTo(340);
        assertThat(pagamentos("SEMANAL")).isEqualTo(2);
        assertThat(medir().semanal()).isEqualTo(70);
        assertThat(xpPersistido()).isEqualTo(340);
        assertThat(
                        jdbc.queryForList(
                                "SELECT inicio_periodo FROM pagamento_desafio_sistema WHERE usuario_id = ? ORDER BY inicio_periodo",
                                LocalDate.class,
                                USUARIO))
                .containsExactly(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 26));
    }

    @Test
    void mensal_nao_repete_e_paga_em_outro_mes_com_outros_livros() {
        registrar(1, total(1));
        registrar(2, total(2));
        int xpAntes = xpPersistido();
        var terceira = registrar(3, total(3));
        assertThat(terceira.xpDoUsuario()).isEqualTo(xpAntes + terceira.xpTotal());
        assertThat(pagamentos("MENSAL")).isEqualTo(1);

        when(clock.instant()).thenReturn(Instant.parse("2026-11-02T15:00:00Z"));
        assertThat(medir().mensal()).isZero();
        registrar(4, total(4));
        int antesDoSegundo = xpPersistido();
        var segundo = registrar(5, total(5));
        assertThat(segundo.xpDoUsuario()).isEqualTo(antesDoSegundo + segundo.xpTotal() + 200);
        assertThat(medir().mensal()).isEqualTo(2);
        assertThat(pagamentos("MENSAL")).isEqualTo(2);
        assertThat(
                        jdbc.queryForList(
                                "SELECT inicio_periodo FROM pagamento_desafio_sistema WHERE usuario_id = ? AND desafio = 'MENSAL' ORDER BY inicio_periodo",
                                LocalDate.class,
                                USUARIO))
                .containsExactly(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1));
    }

    @Test
    void semanal_e_mensal_cruzam_juntos_e_somam_300_de_xp() {
        preparar_cruzamento_duplo();
        var resposta = registrar(2, total(2));
        assertThat(resposta.xpPaginas()).isEqualTo(15);
        assertThat(resposta.xpTotal()).isEqualTo(165);
        assertThat(resposta.xpDoUsuario()).isEqualTo(465);
        assertThat(pagamentos("MENSAL")).isEqualTo(1);
        assertThat(pagamentos("SEMANAL")).isEqualTo(1);
        assertThat(xpPersistido()).isEqualTo(465);
        assertThat(medir()).isEqualTo(new DesafiosSistemaService.Medicao(2, 75));
    }

    @Test
    void recuar_e_voltar_a_maxima_nao_cria_progresso_ou_pagamento() {
        UUID item = item(1, 60);
        registro(item, 60, null, AGORA.minusSeconds(60));
        assertThat(registrar(1, 20).xpTotal()).isZero();
        assertThat(registrar(1, 60).xpTotal()).isZero();
        assertThat(medir().semanal()).isEqualTo(60);
        assertThat(pagamentos("SEMANAL")).isZero();
        assertThat(xpPersistido()).isZero();
    }

    @Test
    void apenas_marcado_lido_nao_completa_mensal_ao_concluir_outro_livro() {
        UUID marcado = item(3, total(3));
        jdbc.update("UPDATE item_biblioteca SET status_leitura = 'lido' WHERE id = ?", marcado);
        registrar(1, total(1));
        assertThat(medir().mensal()).isEqualTo(1);
        assertThat(pagamentos("MENSAL")).isZero();
    }

    @Test
    void primeira_conclusao_em_mes_anterior_nao_conta_ao_reler() {
        Instant setembro = Instant.parse("2026-09-30T15:00:00Z");
        UUID antigo = item(1, total(1));
        registro(antigo, total(1), null, setembro);
        registrar(1, total(1) - 1);
        registrar(1, total(1));
        registrar(2, total(2));
        assertThat(medir().mensal()).isEqualTo(1);
        assertThat(pagamentos("MENSAL")).isZero();
    }

    @Test
    void segunda_em_utc_ainda_domingo_local_e_limite_da_semana() {
        Instant antes = Instant.parse("2026-10-26T02:59:59.999999Z");
        when(clock.instant()).thenReturn(antes);
        registrar(1, 70);
        assertThat(medir().semanal()).isEqualTo(70);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT inicio_periodo FROM pagamento_desafio_sistema WHERE usuario_id = ?",
                                LocalDate.class,
                                USUARIO))
                .isEqualTo(LocalDate.of(2026, 10, 19));

        Instant inicio = Instant.parse("2026-10-26T03:00:00Z");
        when(clock.instant()).thenReturn(inicio);
        assertThat(medir().semanal()).isZero();
        registrar(1, 140);
        assertThat(medir().semanal()).isEqualTo(70);
        assertThat(pagamentos("SEMANAL")).isEqualTo(2);
        conferir_instante_da_leitura_e_pagamento(inicio, "SEMANAL", LocalDate.of(2026, 10, 26));
    }

    @Test
    void virada_do_mes_respeita_sao_paulo_e_primeira_conclusao() {
        Instant antes = Instant.parse("2026-11-01T02:59:59.999999Z");
        when(clock.instant()).thenReturn(antes);
        registrar(1, total(1));
        registrar(2, total(2));
        assertThat(medir().mensal()).isEqualTo(2);
        conferir_instante_da_leitura_e_pagamento(antes, "MENSAL", LocalDate.of(2026, 10, 1));

        Instant inicio = Instant.parse("2026-11-01T03:00:00Z");
        when(clock.instant()).thenReturn(inicio);
        assertThat(medir().mensal()).isZero();
        registrar(1, total(1));
        assertThat(medir().mensal()).isZero();
        registrar(3, total(3));
        registrar(4, total(4));
        assertThat(medir().mensal()).isEqualTo(2);
        assertThat(pagamentos("MENSAL")).isEqualTo(2);
        conferir_instante_da_leitura_e_pagamento(inicio, "MENSAL", LocalDate.of(2026, 11, 1));
    }

    @Test
    void endpoint_preserva_estrutura_e_reflete_subida_por_pagamento() throws Exception {
        UUID item = item(1, 60);
        registro(item, 60, null, AGORA.minusSeconds(60));
        var resultado =
                mvc.post()
                        .uri("/api/livros/{livroId}/progresso", livro(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pagina\":75}")
                        .exchange();
        assertThat(resultado).hasStatusOk();
        var json = mapper.readTree(resultado.getResponse().getContentAsString());
        List<String> campos =
                List.of(
                        "paginaAtual",
                        "paginaMaximaAlcancada",
                        "totalPaginas",
                        "percentual",
                        "xpPaginas",
                        "xpConclusao",
                        "xpTotal",
                        "concluido",
                        "sequenciaDias",
                        "nivel",
                        "xpDoUsuario",
                        "xpNoNivel",
                        "xpDoNivel",
                        "subiuDeNivel");
        assertThat(json.size()).isEqualTo(campos.size());
        for (String campo : campos) {
            assertThat(json.has(campo)).as(campo).isTrue();
        }
        assertThat(json.path("xpTotal").intValue()).isEqualTo(15);
        assertThat(json.path("xpDoUsuario").intValue()).isEqualTo(115);
        assertThat(json.path("nivel").intValue()).isEqualTo(2);
        assertThat(json.path("xpNoNivel").intValue()).isEqualTo(70);
        assertThat(json.path("xpDoNivel").intValue()).isEqualTo(127);
        assertThat(json.path("subiuDeNivel").booleanValue()).isTrue();
        assertThat(xpPersistido()).isEqualTo(115);
    }

    private void preparar_cruzamento_duplo() {
        Instant semanaAnterior = Instant.parse("2026-10-16T15:00:00Z");
        UUID primeiro = item(1, total(1));
        registro(primeiro, total(1), null, semanaAnterior);
        UUID segundo = item(2, total(2) - 15);
        registro(segundo, total(2) - 75, null, semanaAnterior);
        registro(segundo, total(2) - 15, 60, AGORA.minusSeconds(3600));
        assertThat(medir()).isEqualTo(new DesafiosSistemaService.Medicao(1, 60));
    }

    private void conferir_instante_da_leitura_e_pagamento(
            Instant esperado, String desafio, LocalDate inicio) {
        assertThat(
                        jdbc.queryForObject(
                                        "SELECT pago_em FROM pagamento_desafio_sistema WHERE usuario_id = ? AND desafio = CAST(? AS desafio_sistema) AND inicio_periodo = ?",
                                        OffsetDateTime.class,
                                        USUARIO,
                                        desafio,
                                        inicio)
                                .toInstant())
                .isEqualTo(esperado);
        assertThat(
                        jdbc.queryForObject(
                                        "SELECT MAX(r.registrado_em) FROM registro_leitura r JOIN item_biblioteca i ON i.id = r.item_biblioteca_id WHERE i.usuario_id = ?",
                                        OffsetDateTime.class,
                                        USUARIO)
                                .toInstant())
                .isEqualTo(esperado);
    }

    private ProgressoLeituraResponse registrar(int numero, int pagina) {
        return leitura.registrar(livro(numero), new RegistrarProgressoRequest(pagina));
    }

    private DesafiosSistemaService.Medicao medir() {
        Instant instante = clock.instant();
        return desafios.medir(
                USUARIO, instante.atZone(DesafioSistema.FUSO).toLocalDate(), instante);
    }

    private long pagamentos(String desafio) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM pagamento_desafio_sistema WHERE usuario_id = ? AND desafio = CAST(? AS desafio_sistema)",
                Long.class,
                USUARIO,
                desafio);
    }

    private int xpPersistido() {
        entityManager.flush();
        return jdbc.queryForObject(
                "SELECT xp_total FROM usuario WHERE id = ?", Integer.class, USUARIO);
    }

    private UUID livro(int numero) {
        return UUID.fromString("30000000-0000-0000-0000-%012d".formatted(numero));
    }

    private int total(int numero) {
        return jdbc.queryForObject(
                "SELECT total_paginas FROM livro WHERE id = ?", Integer.class, livro(numero));
    }

    private UUID item(int numero, int maxima) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO item_biblioteca (id, usuario_id, livro_id, status_leitura, pagina_atual, pagina_maxima) VALUES (?, ?, ?, 'lendo', ?, ?)",
                id,
                USUARIO,
                livro(numero),
                maxima,
                maxima);
        return id;
    }

    private void registro(UUID item, int pagina, Integer novas, Instant instante) {
        jdbc.update(
                "INSERT INTO registro_leitura (id, item_biblioteca_id, ultima_pagina, paginas_novas, registrado_em) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                item,
                pagina,
                novas,
                instante.atOffset(ZoneOffset.UTC));
    }
}
