package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Título do usuário por nível")
class TituloUsuarioIT {

    @Autowired private MockMvcTester mvc;

    @Autowired private JdbcTemplate jdbc;

    @Test
    void nivel_1_deve_ser_leitor_iniciante() {
        conferirTitulo(1, "Leitor iniciante");
    }

    @Test
    void nivel_10_deve_ser_leitor_iniciante() {
        conferirTitulo(10, "Leitor iniciante");
    }

    @Test
    void nivel_11_deve_ser_leitor_explorador() {
        conferirTitulo(11, "Leitor explorador");
    }

    @Test
    void nivel_20_deve_ser_leitor_explorador() {
        conferirTitulo(20, "Leitor explorador");
    }

    @Test
    void nivel_21_deve_ser_leitor_dedicado() {
        conferirTitulo(21, "Leitor dedicado");
    }

    @Test
    void nivel_30_deve_ser_leitor_dedicado() {
        conferirTitulo(30, "Leitor dedicado");
    }

    @Test
    void nivel_31_deve_ser_leitor_experiente() {
        conferirTitulo(31, "Leitor experiente");
    }

    @Test
    void nivel_40_deve_ser_leitor_experiente() {
        conferirTitulo(40, "Leitor experiente");
    }

    @Test
    void nivel_41_deve_ser_mestre_da_leitura() {
        conferirTitulo(41, "Mestre da leitura");
    }

    @Test
    void nivel_100_deve_continuar_mestre_da_leitura() {
        conferirTitulo(100, "Mestre da leitura");
    }

    private void conferirTitulo(int nivel, String tituloEsperado) {
        jdbc.update(
                """
                UPDATE usuario
                SET nivel = ?
                WHERE id = '00000000-0000-0000-0000-000000000001'
                """,
                nivel);

        assertThat(mvc.get().uri("/api/busca?q=danielribeiro&tipo=usuarios"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens[0].titulo")
                .isEqualTo(tituloEsperado);
    }
}
