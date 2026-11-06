package com.readrace.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Seed coerente da V29 (#220). Sobe o banco até a migration anterior à V29, guarda o estado e
 * aplica a V29, para comparar o antes e o depois.
 */
@Testcontainers
@DisplayName("Seed coerente (V29)")
class SeedCoerenteIT {

    /** Número da migration do seed coerente. Se ela for renomeada, só muda aqui. */
    static final int VERSAO = 29;

    private static final String DANIEL = "00000000-0000-0000-0000-000000000001";
    private static final String MARIANA = "00000000-0000-0000-0000-000000000004";
    private static final String PEDRO = "00000000-0000-0000-0000-000000000009";
    private static final String OS_OUTROS_NOVE = "u.id <> '" + DANIEL + "'";

    /** XP = páginas + 150 por livro lido + recompensas das conquistas + XP de quiz. */
    private static final String XP_PELAS_FONTES =
            """
            (SELECT coalesce(sum(i.pagina_maxima), 0)
                    + 150 * count(*) FILTER (WHERE i.status_leitura = 'lido')
             FROM item_biblioteca i WHERE i.usuario_id = u.id)
            + (SELECT coalesce(sum(c.recompensa_xp), 0) FROM conquista_usuario cu
               JOIN conquista c ON c.id = cu.conquista_id WHERE cu.usuario_id = u.id)
            + (SELECT coalesce(sum(q.xp_pago), 0) FROM conclusao_quiz q
               WHERE q.usuario_id = u.id)
            """;

    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    private static JdbcTemplate jdbc;
    private static List<Map<String, Object>> xpENivelAntes;
    private static long registrosAntes;

    @BeforeAll
    static void migrar() {
        var dataSource =
                new DriverManagerDataSource(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(dataSource);

        Flyway.configure().dataSource(dataSource).target(versaoAnterior()).load().migrate();
        xpENivelAntes = xpENivel();
        registrosAntes = jdbc.queryForObject("SELECT count(*) FROM registro_leitura", Long.class);

        Flyway.configure().dataSource(dataSource).target(String.valueOf(VERSAO)).load().migrate();
    }

    @Test
    void xp_de_cada_um_dos_outros_nove_deve_fechar_com_paginas_livros_e_conquistas() {
        List<String> incoerentes =
                jdbc.queryForList(
                        "SELECT u.nome || ': ' || u.xp_total || ' x ' || ("
                                + XP_PELAS_FONTES
                                + ") FROM usuario u WHERE "
                                + OS_OUTROS_NOVE
                                + " AND u.xp_total <> ("
                                + XP_PELAS_FONTES
                                + ")",
                        String.class);

        assertThat(incoerentes).isEmpty();
    }

    @Test
    void conquistas_dos_outros_nove_devem_ser_exatamente_as_alcancadas() {
        // Os mesmos critérios do ConquistaRepository.
        List<String> divergentes =
                jdbc.queryForList(
                        """
                        WITH medida AS (
                            SELECT u.id AS usuario_id, c.id AS conquista_id,
                                   CASE c.criterio
                                       WHEN 'livros_lidos' THEN (SELECT count(*)
                                           FROM item_biblioteca i WHERE i.usuario_id = u.id
                                           AND i.status_leitura = 'lido')
                                       WHEN 'paginas_lidas' THEN (SELECT coalesce(sum(
                                           i.pagina_maxima), 0) FROM item_biblioteca i
                                           WHERE i.usuario_id = u.id)
                                       WHEN 'desafios' THEN (SELECT count(*) FROM desafio_amigo d
                                           WHERE d.status = 'finalizado'
                                           AND u.id IN (d.criador_id, d.oponente_id))
                                       WHEN 'clubes' THEN (SELECT count(*) FROM membro_clube m
                                           JOIN clube_do_livro cl ON cl.id = m.clube_id
                                           WHERE m.usuario_id = u.id AND cl.excluido_em IS NULL)
                                   END >= c.meta_valor AS alcancou,
                                   EXISTS (SELECT 1 FROM conquista_usuario cu
                                           WHERE cu.usuario_id = u.id
                                             AND cu.conquista_id = c.id) AS tem
                            FROM usuario u CROSS JOIN conquista c
                            WHERE u.id <> ?::uuid)
                        SELECT usuario_id || ' ' || conquista_id FROM medida
                        WHERE alcancou <> tem
                        """,
                        String.class,
                        DANIEL);

        assertThat(divergentes).isEmpty();
    }

    @Test
    void perfil_da_ana_deve_mostrar_livros_paginas_e_conquistas_coerentes_com_o_nivel() {
        Map<String, Object> ana =
                jdbc.queryForMap(
                        """
                        SELECT count(*) FILTER (WHERE status_leitura = 'lido') AS lidos,
                               sum(pagina_maxima) AS paginas,
                               (SELECT count(*) FROM conquista_usuario
                                WHERE usuario_id = '00000000-0000-0000-0000-000000000002')
                                   AS conquistas
                        FROM item_biblioteca
                        WHERE usuario_id = '00000000-0000-0000-0000-000000000002'
                        """);

        assertThat(ana.get("lidos")).isEqualTo(3L);
        assertThat(ana.get("paginas")).isEqualTo(1075L);
        // Primeiros Passos, Mil Páginas e Em Boa Companhia.
        assertThat(ana.get("conquistas")).isEqualTo(3L);
    }

    @Test
    void quem_e_do_clube_3_deve_estar_lendo_o_livro_do_clube() {
        List<String> semOLivroDoClube =
                jdbc.queryForList(
                        """
                        SELECT u.nome FROM membro_clube m JOIN usuario u ON u.id = m.usuario_id
                        WHERE m.clube_id = '50000000-0000-0000-0000-000000000003'
                          AND u.id <> ?::uuid
                          AND NOT EXISTS (
                              SELECT 1 FROM item_biblioteca i
                              WHERE i.usuario_id = u.id
                                AND i.livro_id = '30000000-0000-0000-0000-000000000008'
                                AND i.status_leitura = 'lendo')
                        """,
                        String.class,
                        DANIEL);

        assertThat(semOLivroDoClube).isEmpty();
    }

    @Test
    void xp_nivel_e_registros_de_leitura_nao_devem_mudar() {
        assertThat(xpENivel()).isEqualTo(xpENivelAntes);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM registro_leitura", Long.class))
                .isEqualTo(registrosAntes);
    }

    @Test
    void forum_dos_clubes_so_deve_ter_posts_de_membros() {
        List<String> deForasteiros =
                jdbc.queryForList(
                        """
                        SELECT p.id::text FROM post p
                        WHERE p.clube_id IS NOT NULL
                          AND NOT EXISTS (SELECT 1 FROM membro_clube m
                                          WHERE m.clube_id = p.clube_id
                                            AND m.usuario_id = p.autor_id)
                        """,
                        String.class);

        assertThat(deForasteiros).isEmpty();
        assertThat(
                        jdbc.queryForObject(
                                """
                                SELECT count(*) FROM post
                                WHERE clube_id IN ('50000000-0000-0000-0000-000000000004',
                                                   '50000000-0000-0000-0000-000000000005')
                                  AND autor_id = ?::uuid
                                """,
                                Long.class,
                                PEDRO))
                .isEqualTo(2L);
    }

    @Test
    void ninguem_deve_curtir_o_proprio_post() {
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM curtida c JOIN post p ON p.id = c.post_id"
                                        + " WHERE c.usuario_id = p.autor_id",
                                Long.class))
                .isZero();
    }

    @Test
    void a_metamorfose_deve_ter_a_propria_capa() {
        assertThat(
                        jdbc.queryForObject(
                                "SELECT capa_url FROM livro WHERE titulo = 'A Metamorfose'",
                                String.class))
                .isEqualTo("https://covers.openlibrary.org/b/isbn/9788571646858-L.jpg");
    }

    @Test
    void desafio_de_livro_finalizado_deve_ter_placar_em_paginas_com_a_vencedora_no_fim_do_livro() {
        Map<String, Object> placar =
                jdbc.queryForMap(
                        """
                        SELECT max(valor_atual) FILTER (WHERE usuario_id = ?::uuid) AS daniel,
                               max(valor_atual) FILTER (WHERE usuario_id = ?::uuid) AS mariana
                        FROM progresso_desafio
                        WHERE desafio_id = '90000000-0000-0000-0000-000000000003'
                        """,
                        DANIEL,
                        MARIANA);

        assertThat(placar.get("daniel")).isEqualTo(90);
        assertThat(placar.get("mariana")).isEqualTo(128);
    }

    @Test
    void icones_das_conquistas_devem_usar_versao_fixa_do_twemoji() {
        assertThat(
                        jdbc.queryForList(
                                "SELECT icone_url FROM conquista WHERE icone_url NOT LIKE"
                                        + " 'https://cdn.jsdelivr.net/gh/jdecked/twemoji@17.0.3/%'",
                                String.class))
                .isEmpty();
    }

    /** A maior migration antes da V29: o estado "antes" acompanha as que entrarem na dev. */
    static String versaoAnterior() {
        try (var arquivos =
                java.nio.file.Files.list(
                        java.nio.file.Path.of("src/main/resources/db/migration"))) {
            return arquivos.map(arquivo -> arquivo.getFileName().toString())
                    .map(nome -> Integer.parseInt(nome.substring(1, nome.indexOf("__"))))
                    .filter(versao -> versao < VERSAO)
                    .max(Integer::compare)
                    .orElseThrow()
                    .toString();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static List<Map<String, Object>> xpENivel() {
        return jdbc.queryForList("SELECT id, xp_total, nivel FROM usuario ORDER BY id");
    }
}
