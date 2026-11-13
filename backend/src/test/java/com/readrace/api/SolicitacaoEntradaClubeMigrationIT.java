package com.readrace.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class SolicitacaoEntradaClubeMigrationIT {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void migra_banco_novo_preserva_associacoes_e_cria_quatro_pedidos_elegiveis() {
        var ds =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        // A dev já inclui a V23; isolamos a V28 para conferir que ela preserva o seed atual.
        Flyway.configure().dataSource(ds).target("23").load().migrate();
        var jdbc = new JdbcTemplate(ds);
        var associacoes = jdbc.queryForList("SELECT * FROM membro_clube ORDER BY id");
        var usuarios = jdbc.queryForList("SELECT * FROM usuario ORDER BY id");
        var clubes = jdbc.queryForList("SELECT * FROM clube_do_livro ORDER BY id");

        var flyway = Flyway.configure().dataSource(ds).target("28").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("28");
        assertThat(jdbc.queryForList("SELECT * FROM membro_clube ORDER BY id"))
                .isEqualTo(associacoes);
        assertThat(jdbc.queryForList("SELECT * FROM usuario ORDER BY id")).isEqualTo(usuarios);
        assertThat(jdbc.queryForList("SELECT * FROM clube_do_livro ORDER BY id")).isEqualTo(clubes);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM solicitacao_entrada_clube", Integer.class))
                .isEqualTo(4);
        assertThat(
                        jdbc.queryForObject(
                                """
                SELECT count(*) FROM solicitacao_entrada_clube s
                JOIN clube_do_livro c ON c.id = s.clube_id AND c.excluido_em IS NULL
                JOIN usuario u ON u.id = s.usuario_id AND u.excluido_em IS NULL
                JOIN membro_clube admin ON admin.clube_id = c.id
                    AND admin.usuario_id = '00000000-0000-0000-0000-000000000001'
                    AND admin.cargo_clube = 'administrador'
                JOIN usuario demo ON demo.id = admin.usuario_id AND demo.excluido_em IS NULL
                WHERE s.situacao = 'pendente' AND s.respondida_em IS NULL AND s.administrador_id IS NULL
                  AND NOT EXISTS (SELECT 1 FROM membro_clube m
                      WHERE m.clube_id = s.clube_id AND m.usuario_id = s.usuario_id)
                """,
                                Integer.class))
                .isEqualTo(4);
        for (String clube : new String[] {"1", "2"}) {
            UUID clubeId = UUID.fromString("50000000-0000-0000-0000-00000000000" + clube);
            assertThat(
                            jdbc.queryForList(
                                    """
                    SELECT u.nome FROM solicitacao_entrada_clube s JOIN usuario u ON u.id = s.usuario_id
                    WHERE s.clube_id = ? ORDER BY s.criada_em, s.id
                    """,
                                    String.class,
                                    clubeId))
                    .containsExactly("Pedro Almeida", "Julia Ferreira");
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(DISTINCT criada_em) FROM solicitacao_entrada_clube",
                                Integer.class))
                .isEqualTo(4);
        assertThat(
                        jdbc.queryForList(
                                """
                SELECT clube_id FROM membro_clube
                WHERE usuario_id = '00000000-0000-0000-0000-000000000001' ORDER BY clube_id
                """,
                                UUID.class))
                .containsExactly(
                        UUID.fromString("50000000-0000-0000-0000-000000000001"),
                        UUID.fromString("50000000-0000-0000-0000-000000000002"),
                        UUID.fromString("50000000-0000-0000-0000-000000000003"));
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
}
