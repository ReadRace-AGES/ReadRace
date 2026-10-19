package com.readrace.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class DesafioSistemaTest {
    @Test
    void semana_comeca_na_segunda_e_termina_na_segunda_seguinte() {
        var domingo = DesafioSistema.SEMANAL.periodo(LocalDate.of(2026, 10, 18));
        assertThat(domingo.inicioEm()).isEqualTo(Instant.parse("2026-10-12T03:00:00Z"));
        assertThat(domingo.fimEm()).isEqualTo(Instant.parse("2026-10-19T03:00:00Z"));
        assertThat(domingo.diasRestantes(LocalDate.of(2026, 10, 18))).isEqualTo(1);
        var segunda = DesafioSistema.SEMANAL.periodo(LocalDate.of(2026, 10, 19));
        assertThat(segunda.inicioEm()).isEqualTo(domingo.fimEm());
        assertThat(segunda.diasRestantes(LocalDate.of(2026, 10, 19))).isEqualTo(7);
    }

    @Test
    void calendario_respeita_virada_do_ano_e_fevereiro_bissexto() {
        var dezembro = DesafioSistema.MENSAL.periodo(LocalDate.of(2026, 12, 31));
        assertThat(dezembro.fimEm()).isEqualTo(Instant.parse("2027-01-01T03:00:00Z"));
        assertThat(dezembro.diasRestantes(LocalDate.of(2026, 12, 31))).isEqualTo(1);
        var fevereiro = DesafioSistema.MENSAL.periodo(LocalDate.of(2024, 2, 1));
        assertThat(fevereiro.diasRestantes(LocalDate.of(2024, 2, 1))).isEqualTo(29);
        assertThat(fevereiro.fimExclusivo()).isEqualTo(LocalDate.of(2024, 3, 1));
        var semana = DesafioSistema.SEMANAL.periodo(LocalDate.of(2027, 1, 1));
        assertThat(semana.inicio()).isEqualTo(LocalDate.of(2026, 12, 28));
        assertThat(semana.fimExclusivo()).isEqualTo(LocalDate.of(2027, 1, 4));
    }
}
