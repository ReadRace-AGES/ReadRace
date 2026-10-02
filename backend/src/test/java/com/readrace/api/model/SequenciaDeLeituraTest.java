package com.readrace.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SequenciaDeLeituraTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 28);

    @Test
    void deve_comecar_em_um_no_primeiro_registro() {
        assertThat(SequenciaDeLeitura.aposLeitura(0, null, HOJE)).isEqualTo(1);
    }

    @ParameterizedTest(name = "última leitura há {0} dias: {1} vira {2}")
    @CsvSource({"0, 13, 13", "1, 12, 13", "4, 12, 13", "7, 12, 13", "8, 12, 1", "30, 12, 1"})
    void deve_seguir_a_tabela_da_issue_96_ao_registrar(
            int diasSemLer, int sequencia, int esperada) {
        LocalDate ultimaLeitura = HOJE.minusDays(diasSemLer);

        assertThat(SequenciaDeLeitura.aposLeitura(sequencia, ultimaLeitura, HOJE))
                .isEqualTo(esperada);
    }

    @ParameterizedTest(name = "última leitura há {0} dias: exibe {1}")
    @CsvSource({"0, 12", "1, 12", "7, 12", "8, 0", "30, 0"})
    void deve_exibir_zero_so_depois_de_oito_dias_sem_leitura(int diasSemLer, int esperada) {
        assertThat(SequenciaDeLeitura.exibida(12, HOJE.minusDays(diasSemLer), HOJE))
                .isEqualTo(esperada);
    }

    @Test
    void deve_exibir_zero_para_quem_nunca_leu() {
        assertThat(SequenciaDeLeitura.exibida(0, null, HOJE)).isZero();
    }

    @Test
    void deve_tratar_data_no_futuro_como_hoje() {
        LocalDate futuro = HOJE.plusDays(3);

        assertThat(SequenciaDeLeitura.aposLeitura(12, futuro, HOJE)).isEqualTo(12);
        assertThat(SequenciaDeLeitura.exibida(12, futuro, HOJE)).isEqualTo(12);
    }

    @Test
    void deve_usar_o_dia_de_sao_paulo() {
        assertThat(SequenciaDeLeitura.FUSO).isEqualTo(ZoneId.of("America/Sao_Paulo"));
    }
}
