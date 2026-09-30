package com.readrace.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CurvaDeNivelTest {

    @ParameterizedTest(name = "nível {0}: acumulado {1}, exige {2}")
    @CsvSource({
        "1, 0, 45", "2, 45, 127", "3, 172, 233", "4, 405, 360",
        "5, 765, 503", "6, 1268, 661", "7, 1929, 833", "8, 2762, 1018"
    })
    void segueATabelaDaIssue95(int nivel, int acumulado, int exigido) {
        assertThat(CurvaDeNivel.xpParaChegarNoNivel(nivel)).isEqualTo(acumulado);
        assertThat(CurvaDeNivel.xpDoNivel(nivel)).isEqualTo(exigido);
    }

    @Test
    void xpNoNivelEORestoDentroDoNivel() {
        assertThat(CurvaDeNivel.xpNoNivel(2505, 7)).isEqualTo(576);
    }

    @ParameterizedTest(name = "{0} de XP é nível {1}")
    @CsvSource({"0, 1", "44, 1", "45, 2", "171, 2", "172, 3", "2761, 7", "2762, 8", "2800, 8"})
    void derivaONivelDoXpTotal(int xpTotal, int nivel) {
        assertThat(CurvaDeNivel.nivelDoXp(xpTotal)).isEqualTo(nivel);
    }
}
