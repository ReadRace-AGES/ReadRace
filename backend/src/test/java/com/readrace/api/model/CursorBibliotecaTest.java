package com.readrace.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.readrace.api.exception.ParametroInvalidoException;

@DisplayName("CursorBiblioteca")
class CursorBibliotecaTest {

    @Test
    void deve_codificar_e_decodificar_sem_perder_precisao() {
        CursorBiblioteca cursor =
                new CursorBiblioteca(
                        Instant.parse("2026-09-11T22:07:56.123456Z"),
                        UUID.fromString("40000000-0000-0000-0000-000000000007"));

        assertThat(CursorBiblioteca.decodificar(cursor.codificar())).isEqualTo(cursor);
    }

    @Test
    void deve_gerar_texto_opaco_e_seguro_para_url() {
        String codificado = CursorBiblioteca.INICIO.codificar();

        assertThat(codificado).matches("[A-Za-z0-9_-]+");
        assertThat(codificado).doesNotContain("|", "=");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "!!!", "MjAyNi0wOS0xMVQwMDowMDowMFo", "bmFvLWUtZGF0YXwxMjM"})
    void deve_recusar_cursor_que_nao_foi_gerado_pela_api(String cursor) {
        assertThatThrownBy(() -> CursorBiblioteca.decodificar(cursor))
                .isInstanceOf(ParametroInvalidoException.class)
                .hasMessageContaining("cursor");
    }
}
