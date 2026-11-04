package com.readrace.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Na produção o estado dos usuários pode ser outro que o de um banco novo. Quando a conta de XP de
 * alguém não fecha, a V25 pula essa pessoa em vez de falhar: uma migration com erro impede a API de
 * subir no deploy (#220).
 */
@Testcontainers
@DisplayName("Seed coerente (V25) com XP que não fecha")
class SeedCoerentePularIT {

    private static final String JULIA = "00000000-0000-0000-0000-000000000010";

    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void deve_pular_quem_nao_fecha_a_conta_sem_falhar_a_migration() {
        var dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var jdbc = new JdbcTemplate(dataSource);
        Flyway.configure()
                .dataSource(dataSource)
                .target(SeedCoerenteIT.versaoAnteriorAV25())
                .load()
                .migrate();
        // Menos que os 75 de "Em Boa Companhia", que ela alcança por estar em clubes.
        jdbc.update("UPDATE usuario SET xp_total = 10 WHERE id = ?::uuid", JULIA);

        var resultado = Flyway.configure().dataSource(dataSource).target("25").load().migrate();

        assertThat(resultado.success).isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM item_biblioteca WHERE usuario_id = ?::uuid",
                                Long.class,
                                JULIA))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM conquista_usuario WHERE usuario_id = ?::uuid",
                                Long.class,
                                JULIA))
                .isZero();
        // Os outros seguem normalmente.
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM item_biblioteca"
                                        + " WHERE usuario_id = '00000000-0000-0000-0000-000000000002'",
                                Long.class))
                .isEqualTo(4L);
    }
}
