package com.readrace.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.model.DesafioSistema;
import com.readrace.api.model.Usuario;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class PagamentoDesafioSistemaRepositoryIT {
    private static final UUID USUARIO = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OUTRO = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ITEM = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final LocalDate INICIO = LocalDate.of(2026, 10, 19);
    private static final OffsetDateTime PAGO_EM = OffsetDateTime.parse("2026-10-24T12:00:00-03:00");

    @Autowired PagamentoDesafioSistemaRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void deve_inserir_mapear_e_preservar_pagamento_original_na_repeticao() {
        UUID id = UUID.randomUUID();
        Usuario usuario = entityManager.find(Usuario.class, USUARIO);
        assertThat(repository.registrarSeAusente(id, USUARIO, "SEMANAL", INICIO, 100, PAGO_EM))
                .isEqualTo(1);
        assertThat(entityManager.contains(usuario)).isTrue();
        assertThat(
                        repository.registrarSeAusente(
                                UUID.randomUUID(),
                                USUARIO,
                                "SEMANAL",
                                INICIO,
                                999,
                                PAGO_EM.plusDays(1)))
                .isZero();

        var pagamento = repository.findById(id).orElseThrow();
        assertThat(pagamento.getId()).isEqualTo(id);
        assertThat(pagamento.getUsuario().getId()).isEqualTo(USUARIO);
        assertThat(pagamento.getDesafio()).isEqualTo(DesafioSistema.SEMANAL);
        assertThat(pagamento.getInicioPeriodo()).isEqualTo(INICIO);
        assertThat(pagamento.getXpPago()).isEqualTo(100);
        assertThat(pagamento.getPagoEm().toInstant()).isEqualTo(PAGO_EM.toInstant());
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void deve_permitir_outro_periodo_desafio_ou_usuario() {
        assertThat(
                        repository.registrarSeAusente(
                                UUID.randomUUID(), USUARIO, "SEMANAL", INICIO, 100, PAGO_EM))
                .isEqualTo(1);
        assertThat(
                        repository.registrarSeAusente(
                                UUID.randomUUID(),
                                USUARIO,
                                "SEMANAL",
                                INICIO.plusWeeks(1),
                                100,
                                PAGO_EM))
                .isEqualTo(1);
        UUID mensal = UUID.randomUUID();
        assertThat(repository.registrarSeAusente(mensal, USUARIO, "MENSAL", INICIO, 200, PAGO_EM))
                .isEqualTo(1);
        assertThat(repository.findById(mensal).orElseThrow().getDesafio())
                .isEqualTo(DesafioSistema.MENSAL);
        assertThat(
                        repository.registrarSeAusente(
                                UUID.randomUUID(), OUTRO, "SEMANAL", INICIO, 100, PAGO_EM))
                .isEqualTo(1);
        assertThat(repository.count()).isEqualTo(4);
    }

    // Cada invocação tem sua própria transação de teste, desfeita após a falha SQL.
    @ParameterizedTest
    @MethodSource("pagamentosInvalidos")
    void deve_rejeitar_pagamento_invalido(
            UUID id,
            UUID usuario,
            String desafio,
            LocalDate inicio,
            Integer xp,
            OffsetDateTime pago) {
        assertThatThrownBy(
                        () -> repository.registrarSeAusente(id, usuario, desafio, inicio, xp, pago))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    static Stream<Arguments> pagamentosInvalidos() {
        UUID id = UUID.randomUUID();
        return Stream.of(
                Arguments.of(id, USUARIO, "SEMANAL", INICIO, 0, PAGO_EM),
                Arguments.of(id, USUARIO, "SEMANAL", INICIO, -1, PAGO_EM),
                Arguments.of(id, UUID.randomUUID(), "SEMANAL", INICIO, 100, PAGO_EM),
                Arguments.of(null, USUARIO, "SEMANAL", INICIO, 100, PAGO_EM),
                Arguments.of(id, null, "SEMANAL", INICIO, 100, PAGO_EM),
                Arguments.of(id, USUARIO, null, INICIO, 100, PAGO_EM),
                Arguments.of(id, USUARIO, "SEMANAL", null, 100, PAGO_EM),
                Arguments.of(id, USUARIO, "SEMANAL", INICIO, null, PAGO_EM),
                Arguments.of(id, USUARIO, "SEMANAL", INICIO, 100, null));
    }

    @Test
    void conflito_de_chave_primaria_deve_propagar() {
        UUID id = UUID.randomUUID();
        repository.registrarSeAusente(id, USUARIO, "SEMANAL", INICIO, 100, PAGO_EM);
        assertThatThrownBy(
                        () ->
                                repository.registrarSeAusente(
                                        id, USUARIO, "SEMANAL", INICIO.plusWeeks(1), 100, PAGO_EM))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void historico_e_insert_sem_coluna_devem_manter_paginas_novas_nulas() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM registro_leitura", Long.class))
                .isPositive();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM registro_leitura WHERE paginas_novas IS NOT NULL",
                                Long.class))
                .isZero();
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO registro_leitura (id, item_biblioteca_id, ultima_pagina) VALUES (?, ?, ?)",
                id,
                ITEM,
                150);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT paginas_novas FROM registro_leitura WHERE id = ?",
                                Integer.class,
                                id))
                .isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 15})
    void deve_aceitar_paginas_novas_nulas_zero_ou_positivas(Integer paginas) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO registro_leitura (id, item_biblioteca_id, ultima_pagina, paginas_novas) VALUES (?, ?, ?, ?)",
                id,
                ITEM,
                150,
                paginas);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT paginas_novas FROM registro_leitura WHERE id = ?",
                                Integer.class,
                                id))
                .isEqualTo(paginas);
    }

    @Test
    void deve_rejeitar_paginas_novas_negativas() {
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO registro_leitura (id, item_biblioteca_id, ultima_pagina, paginas_novas) VALUES (?, ?, ?, ?)",
                                        UUID.randomUUID(),
                                        ITEM,
                                        150,
                                        -1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
