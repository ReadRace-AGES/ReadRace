package com.readrace.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Roda o script de produção {@code infra/demo/refrescar_demo.sql} (#220) contra um banco com todas
 * as migrations e o seed envelhecido 30 dias, como está a produção hoje.
 *
 * <p>Os testes compartilham o mesmo banco e rodam o script de novo a cada um: é o mesmo uso da
 * produção, onde ele roda mais de uma vez.
 */
@Testcontainers
@DisplayName("Script de refrescar a demo")
class RefrescarDemoIT {

    private static final Path SCRIPT = Path.of("..", "infra", "demo", "refrescar_demo.sql");
    private static final String DANIEL = "00000000-0000-0000-0000-000000000001";
    private static final String DESAFIO_DANIEL_ANA = "90000000-0000-0000-0000-000000000001";
    private static final UUID USUARIO_DO_APP = UUID.randomUUID();
    private static final UUID POST_DO_APP = UUID.randomUUID();

    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    private static JdbcTemplate jdbc;
    private static DriverManagerDataSource dataSource;

    @BeforeAll
    static void montarBancoEnvelhecido() {
        dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbc = new JdbcTemplate(dataSource);

        // O seed de 30 dias atrás: sequência quebrada, desafio vencido e posts antigos.
        jdbc.update("UPDATE usuario SET ultima_leitura_em = ultima_leitura_em - 30");
        jdbc.update(
                "UPDATE registro_leitura SET registrado_em = registrado_em - INTERVAL '30 days'");
        jdbc.update(
                """
                UPDATE post SET criado_em = criado_em - INTERVAL '30 days',
                                atualizado_em = atualizado_em - INTERVAL '30 days'
                """);
        jdbc.update(
                """
                UPDATE desafio_amigo SET status = 'finalizado',
                    inicio_em = inicio_em - INTERVAL '30 days', fim_em = fim_em - INTERVAL '30 days'
                WHERE id = ?::uuid
                """,
                DESAFIO_DANIEL_ANA);
        jdbc.update(
                "UPDATE progresso_desafio SET valor_atual = 300 WHERE desafio_id = ?::uuid",
                DESAFIO_DANIEL_ANA);

        // Dados criados pelo app: um usuário novo e um post que a conta demo fez como Daniel.
        jdbc.update(
                """
                INSERT INTO usuario (id, nome, nome_usuario, email, dias_consecutivos,
                                     ultima_leitura_em)
                VALUES (?, 'Pessoa do App', 'pessoa_do_app', 'pessoa@example.com', 3,
                        DATE '2026-10-01')
                """,
                USUARIO_DO_APP);
        jdbc.update(
                """
                INSERT INTO post (id, autor_id, comunidade_id, conteudo, criado_em, atualizado_em)
                VALUES (?, ?::uuid, '60000000-0000-0000-0000-000000000001', 'Post feito no app',
                        TIMESTAMPTZ '2026-10-01 12:00:00+00', TIMESTAMPTZ '2026-10-01 12:00:00+00')
                """,
                POST_DO_APP,
                DANIEL);
    }

    @Test
    void deve_deixar_a_sequencia_do_usuario_demo_ativa_com_leitura_ontem() {
        refrescar();

        LocalDate ontem = LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1);
        Map<String, Object> daniel =
                jdbc.queryForMap(
                        "SELECT dias_consecutivos, ultima_leitura_em FROM usuario WHERE id = ?::uuid",
                        DANIEL);

        assertThat(daniel.get("dias_consecutivos")).isEqualTo(12);
        assertThat(((java.sql.Date) daniel.get("ultima_leitura_em")).toLocalDate())
                .isEqualTo(ontem);
    }

    @Test
    void deve_deixar_um_desafio_do_usuario_demo_em_andamento_perto_da_meta() {
        refrescar();

        Map<String, Object> desafio =
                jdbc.queryForMap(
                        """
                        SELECT status::text AS status,
                               fim_em > now() + INTERVAL '9 days' AS prazo_cobre_a_banca,
                               inicio_em < now() AS ja_comecou
                        FROM desafio_amigo WHERE id = ?::uuid
                        """,
                        DESAFIO_DANIEL_ANA);
        assertThat(desafio.get("status")).isEqualTo("ativo");
        assertThat(desafio.get("prazo_cobre_a_banca")).isEqualTo(true);
        assertThat(desafio.get("ja_comecou")).isEqualTo(true);

        Map<String, Integer> placar =
                jdbc
                        .queryForList(
                                "SELECT usuario_id::text AS usuario, valor_atual FROM"
                                        + " progresso_desafio WHERE desafio_id = ?::uuid",
                                DESAFIO_DANIEL_ANA)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        linha -> (String) linha.get("usuario"),
                                        linha -> (Integer) linha.get("valor_atual")));
        assertThat(placar)
                .containsEntry(DANIEL, 240)
                .containsEntry("00000000-0000-0000-0000-000000000002", 210);
    }

    @Test
    void deve_trazer_leituras_posts_e_comentarios_do_seed_para_os_ultimos_dias() {
        refrescar();

        assertThat(
                        jdbc.queryForObject(
                                """
                                SELECT max(registrado_em) > now() - INTERVAL '2 days'
                                   AND min(registrado_em) > now() - INTERVAL '19 days'
                                FROM registro_leitura
                                WHERE id::text LIKE '41000000-0000-0000-0000-%'
                                """,
                                Boolean.class))
                .isTrue();

        // Em cada comunidade, o post mais recente do seed é de poucas horas atrás.
        assertThat(
                        jdbc.queryForObject(
                                """
                                SELECT count(*) FROM (
                                    SELECT comunidade_id, max(criado_em) AS recente FROM post
                                    WHERE comunidade_id IS NOT NULL AND id <> ?
                                    GROUP BY comunidade_id) c
                                WHERE c.recente > now() - INTERVAL '12 hours'
                                """,
                                Integer.class,
                                POST_DO_APP))
                .isEqualTo(5);

        assertThat(
                        jdbc.queryForObject(
                                """
                                SELECT count(*) FROM post
                                WHERE id <> ? AND criado_em < now() - INTERVAL '12 days'
                                """,
                                Integer.class,
                                POST_DO_APP))
                .isZero();

        // Comentário nunca fica antes do post que ele comenta.
        assertThat(
                        jdbc.queryForObject(
                                """
                                SELECT count(*) FROM post filho JOIN post pai ON pai.id = filho.post_pai_id
                                WHERE filho.criado_em <= pai.criado_em
                                """,
                                Integer.class))
                .isZero();
    }

    @Test
    void deve_chegar_ao_mesmo_resultado_ao_rodar_de_novo() {
        refrescar();
        Map<String, Double> primeira = datasDoSeed();

        refrescar();
        Map<String, Double> segunda = datasDoSeed();

        assertThat(segunda.keySet()).isEqualTo(primeira.keySet());
        // Entre as duas rodadas só passa o tempo do próprio teste.
        primeira.forEach(
                (id, epoca) -> assertThat(segunda.get(id)).as(id).isCloseTo(epoca, within(60.0)));
    }

    @Test
    void nao_deve_mexer_em_dados_criados_pelo_app() {
        refrescar();
        refrescar();

        Map<String, Object> usuario =
                jdbc.queryForMap(
                        "SELECT dias_consecutivos, ultima_leitura_em::text AS ultima FROM usuario"
                                + " WHERE id = ?",
                        USUARIO_DO_APP);
        assertThat(usuario.get("dias_consecutivos")).isEqualTo(3);
        assertThat(usuario.get("ultima")).isEqualTo("2026-10-01");

        assertThat(
                        jdbc.queryForObject(
                                "SELECT criado_em = TIMESTAMPTZ '2026-10-01 12:00:00+00' FROM post"
                                        + " WHERE id = ?",
                                Boolean.class,
                                POST_DO_APP))
                .isTrue();
    }

    private static org.assertj.core.data.Offset<Double> within(double valor) {
        return org.assertj.core.data.Offset.offset(valor);
    }

    private static void refrescar() {
        new ResourceDatabasePopulator(new FileSystemResource(SCRIPT)).execute(dataSource);
    }

    private static Map<String, Double> datasDoSeed() {
        return jdbc
                .queryForList(
                        """
                        SELECT id::text AS id, extract(epoch FROM criado_em)::float8 AS epoca
                        FROM post
                        UNION ALL
                        SELECT id::text, extract(epoch FROM registrado_em)::float8
                        FROM registro_leitura
                        UNION ALL
                        SELECT id::text, extract(epoch FROM fim_em)::float8 FROM desafio_amigo
                        """)
                .stream()
                .collect(
                        Collectors.toMap(
                                linha -> (String) linha.get("id"),
                                linha -> (Double) linha.get("epoca")));
    }
}
