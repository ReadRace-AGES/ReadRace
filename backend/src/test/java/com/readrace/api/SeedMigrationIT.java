package com.readrace.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class SeedMigrationIT {

    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void deve_atualizar_banco_na_v4_preservando_historico_e_reativando_trigger() {
        var dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Flyway.configure().dataSource(dataSource).target("4").load().migrate();
        var jdbc = new JdbcTemplate(dataSource);
        assertThat(paginaMaxima(jdbc, "001")).isEqualTo(256);

        // Um avanço após o seed não deve ser sobrescrito.
        jdbc.update(
                """
                UPDATE item_biblioteca SET pagina_atual = 220
                WHERE id = '40000000-0000-0000-0000-000000000002'
                """);
        // Mesmo se a página atual voltou ao valor do seed, preserva-se o histórico.
        jdbc.update(
                """
                INSERT INTO registro_leitura (id, item_biblioteca_id, ultima_pagina)
                VALUES (gen_random_uuid(), '40000000-0000-0000-0000-000000000006', 400)
                """);

        Flyway flyway = Flyway.configure().dataSource(dataSource).target("5").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        flyway.validate();

        assertThat(paginaMaxima(jdbc, "001")).isEqualTo(145);
        assertThat(paginaMaxima(jdbc, "002")).isEqualTo(328);
        assertThat(paginaMaxima(jdbc, "006")).isEqualTo(400);
        assertThat(paginaMaxima(jdbc, "010")).isEqualTo(94);
        for (String sufixo : new String[] {"005", "008", "011"}) {
            assertThat(paginaMaxima(jdbc, sufixo)).isZero();
        }
        assertThat(
                        jdbc.queryForObject(
                                """
                SELECT count(*) FROM item_biblioteca i JOIN livro l ON l.id = i.livro_id
                WHERE i.status_leitura = 'lido' AND i.pagina_maxima = l.total_paginas
                """,
                                Integer.class))
                .isEqualTo(5);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        """
                UPDATE item_biblioteca SET pagina_maxima = 1
                WHERE id = '40000000-0000-0000-0000-000000000001'
                """))
                .hasMessageContaining("A página máxima não pode diminuir");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    private int paginaMaxima(JdbcTemplate jdbc, String sufixo) {
        return jdbc.queryForObject(
                "SELECT pagina_maxima FROM item_biblioteca WHERE id = ?::uuid",
                Integer.class,
                "40000000-0000-0000-0000-000000000" + sufixo);
    }
}
