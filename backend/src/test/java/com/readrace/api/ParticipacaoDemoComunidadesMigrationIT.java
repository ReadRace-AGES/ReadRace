package com.readrace.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class ParticipacaoDemoComunidadesMigrationIT {
    private static final String DEMO = "00000000-0000-0000-0000-000000000001";
    private static final String COMUNIDADE = "60000000-0000-0000-0000-00000000000";
    private static final int VERSAO = 30;
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Test
    void banco_novo_mantem_tres_participacoes_e_autores_membros_nas_comunidades_sugeridas() {
        var banco = bancoAte(VERSAO);
        verificarParticipacoes(banco.jdbc());
        assertThat(
                        banco.jdbc()
                                .queryForObject(
                                        "SELECT count(*) FROM membro_comunidade WHERE comunidade_id IN (?::uuid, ?::uuid)",
                                        Long.class,
                                        COMUNIDADE + "4",
                                        COMUNIDADE + "5"))
                .isEqualTo(18L);
        assertThat(
                        banco.jdbc()
                                .queryForObject(
                                        """
                WITH RECURSIVE thread AS (
                    SELECT id, autor_id, comunidade_id FROM post
                    WHERE post_pai_id IS NULL AND comunidade_id IN (?::uuid, ?::uuid)
                    UNION
                    SELECT p.id, p.autor_id, t.comunidade_id FROM post p
                    JOIN thread t ON p.post_pai_id = t.id
                )
                SELECT count(*) FROM thread t
                WHERE NOT EXISTS (SELECT 1 FROM membro_comunidade mc
                                  JOIN usuario u ON u.id = mc.usuario_id
                                  WHERE mc.comunidade_id = t.comunidade_id
                                    AND mc.usuario_id = t.autor_id AND u.excluido_em IS NULL)
                """,
                                        Long.class,
                                        COMUNIDADE + "4",
                                        COMUNIDADE + "5"))
                .isZero();
    }

    @Test
    void atualiza_v29_corrigindo_sete_autorias_e_preservando_todos_os_demais_dados() {
        var banco = bancoAte(29);
        var jdbc = banco.jdbc();
        Map<UUID, UUID> transferencias = transferenciasDoSeed(jdbc);
        var autoresAntes = autores(jdbc);
        assertThat(transferencias).hasSize(7);
        transferencias
                .keySet()
                .forEach(id -> assertThat(autoresAntes.get(id)).isEqualTo(UUID.fromString(DEMO)));
        var postsAntes = postsSemAutor(jdbc);
        var membrosAntes = jdbc.queryForList("SELECT * FROM membro_comunidade ORDER BY id");
        var outrosAntes = outrasTabelas(jdbc);

        var flyway = banco.flyway(VERSAO);
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        flyway.validate();

        var esperados = new LinkedHashMap<>(autoresAntes);
        esperados.putAll(transferencias);
        assertThat(autores(jdbc)).isEqualTo(esperados);
        assertThat(postsSemAutor(jdbc)).isEqualTo(postsAntes);
        assertThat(jdbc.queryForList("SELECT * FROM membro_comunidade ORDER BY id"))
                .isEqualTo(
                        membrosAntes.stream()
                                .filter(
                                        m ->
                                                !(UUID.fromString(DEMO).equals(m.get("usuario_id"))
                                                        && List.of(
                                                                        UUID.fromString(
                                                                                COMUNIDADE + "4"),
                                                                        UUID.fromString(
                                                                                COMUNIDADE + "5"))
                                                                .contains(m.get("comunidade_id"))))
                                .toList());
        assertThat(outrasTabelas(jdbc)).isEqualTo(outrosAntes);
        verificarParticipacoes(jdbc);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"usuario_excluido", "sem_vinculo", "sem_cargo"})
    void inconsistencia_no_segundo_destino_falha_e_reverte_toda_a_migration(String caso) {
        var banco = bancoAte(29);
        var jdbc = banco.jdbc();
        // O segundo destino é validado depois de processar o primeiro: a falha deve reverter ambos.
        String administrador = "00000000-0000-0000-0000-000000000005";
        switch (caso) {
            case "usuario_excluido" ->
                    jdbc.update(
                            "UPDATE usuario SET excluido_em = now() WHERE id = ?::uuid",
                            administrador);
            case "sem_vinculo" ->
                    jdbc.update(
                            "DELETE FROM membro_comunidade WHERE comunidade_id = ?::uuid AND usuario_id = ?::uuid",
                            COMUNIDADE + "5",
                            administrador);
            case "sem_cargo" ->
                    jdbc.update(
                            "UPDATE membro_comunidade SET cargo_comunidade = 'membro' WHERE comunidade_id = ?::uuid AND usuario_id = ?::uuid",
                            COMUNIDADE + "5",
                            administrador);
            default -> throw new IllegalArgumentException(caso);
        }
        var autoresAntes = autores(jdbc);
        var postsAntes = postsSemAutor(jdbc);
        var membrosAntes = jdbc.queryForList("SELECT * FROM membro_comunidade ORDER BY id");

        assertThatThrownBy(() -> banco.flyway(VERSAO).migrate())
                .hasStackTraceContaining("V30: administrador ativo e participante ausente");

        assertThat(autores(jdbc)).isEqualTo(autoresAntes);
        assertThat(postsSemAutor(jdbc)).isEqualTo(postsAntes);
        assertThat(jdbc.queryForList("SELECT * FROM membro_comunidade ORDER BY id"))
                .isEqualTo(membrosAntes);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT max(version::integer) FROM flyway_schema_history WHERE success",
                                Integer.class))
                .isEqualTo(29);
    }

    private static void verificarParticipacoes(JdbcTemplate jdbc) {
        assertThat(
                        jdbc.queryForList(
                                "SELECT comunidade_id::text FROM membro_comunidade WHERE usuario_id = ?::uuid ORDER BY comunidade_id",
                                String.class,
                                DEMO))
                .containsExactly(COMUNIDADE + "1", COMUNIDADE + "2", COMUNIDADE + "3");
        assertThat(
                        jdbc.queryForList(
                                "SELECT comunidade_id::text FROM membro_comunidade WHERE usuario_id = ?::uuid AND cargo_comunidade = 'administrador'",
                                String.class,
                                DEMO))
                .containsExactly(COMUNIDADE + "1");
    }

    private static Map<UUID, UUID> transferenciasDoSeed(JdbcTemplate jdbc) {
        var transferencias = new LinkedHashMap<UUID, UUID>();
        for (String chave :
                List.of(
                        "post-comunidade-4-4",
                        "comentario-comunidade-4-1-3",
                        "comentario-comunidade-4-2-2",
                        "comentario-comunidade-4-3-1",
                        "post-comunidade-5-2",
                        "comentario-comunidade-5-1-1",
                        "resposta-comunidade-5-2")) {
            UUID id = jdbc.queryForObject("SELECT md5(?)::uuid", UUID.class, chave);
            int destino = chave.contains("comunidade-4-") ? 4 : 5;
            transferencias.put(
                    id, UUID.fromString("00000000-0000-0000-0000-00000000000" + destino));
        }
        return transferencias;
    }

    private static Map<UUID, UUID> autores(JdbcTemplate jdbc) {
        var resultado = new LinkedHashMap<UUID, UUID>();
        jdbc.query(
                "SELECT id, autor_id FROM post ORDER BY id",
                rs -> {
                    resultado.put(
                            rs.getObject("id", UUID.class), rs.getObject("autor_id", UUID.class));
                });
        return resultado;
    }

    private static List<String> postsSemAutor(JdbcTemplate jdbc) {
        return jdbc.queryForList(
                "SELECT (to_jsonb(p) - 'autor_id')::text FROM post p ORDER BY id", String.class);
    }

    private static Map<String, List<String>> outrasTabelas(JdbcTemplate jdbc) {
        var resultado = new LinkedHashMap<String, List<String>>();
        for (String tabela :
                jdbc.queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema = current_schema() AND table_type = 'BASE TABLE' AND table_name NOT IN ('post', 'membro_comunidade', 'flyway_schema_history') ORDER BY table_name",
                        String.class)) {
            resultado.put(
                    tabela,
                    jdbc.queryForList(
                            "SELECT to_jsonb(t)::text FROM "
                                    + tabela
                                    + " t ORDER BY to_jsonb(t)::text",
                            String.class));
        }
        return resultado;
    }

    private static Banco bancoAte(int versao) {
        String schema = "comunidades_" + UUID.randomUUID().toString().replace("-", "");
        String url = postgres.getJdbcUrl();
        var dataSource =
                new DriverManagerDataSource(
                        url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                        postgres.getUsername(),
                        postgres.getPassword());
        var banco = new Banco(dataSource, schema);
        banco.flyway(versao).migrate();
        return banco;
    }

    private record Banco(DriverManagerDataSource dataSource, String schema) {
        JdbcTemplate jdbc() {
            return new JdbcTemplate(dataSource);
        }

        Flyway flyway(int versao) {
            return Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .target(String.valueOf(versao))
                    .load();
        }
    }
}
