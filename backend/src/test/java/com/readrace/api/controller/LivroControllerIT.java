package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.service.LivroService;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LivroControllerIT {
    private static final String DOM_CASMURRO = "/api/livros/30000000-0000-0000-0000-000000000001";
    @Autowired private MockMvcTester mvc;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory emf;
    @Autowired private LivroService service;

    @Test
    @Transactional
    void deve_escolher_genero_por_nome_com_multiplos_vinculos() {
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM livro_genero WHERE livro_id = '30000000-0000-0000-0000-000000000001'",
                                Integer.class))
                .isGreaterThan(1);
        assertThat(mvc.get().uri(DOM_CASMURRO))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.livro.genero")
                .isEqualTo("Clássico");
        jdbc.update(
                "INSERT INTO genero (id, nome) VALUES ('29000000-0000-0000-0000-000000000001', 'Aventura de teste')");
        jdbc.update(
                "INSERT INTO livro_genero VALUES ('30000000-0000-0000-0000-000000000001', '29000000-0000-0000-0000-000000000001')");
        assertThat(mvc.get().uri(DOM_CASMURRO))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.livro.genero")
                .isEqualTo("Aventura de teste");
    }

    @Test
    @Transactional
    void deve_manter_quantidade_de_consultas_ao_adicionar_posts() {
        UUID livroId = UUID.fromString("30000000-0000-0000-0000-000000000001");
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        boolean habilitado = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        try {
            em.clear();
            stats.clear();
            var antes = service.buscarDetalhe(livroId);
            long consultas = stats.getPrepareStatementCount();
            assertThat(antes.posts()).hasSizeGreaterThanOrEqualTo(2);
            jdbc.update(
                    """
                INSERT INTO post (id, autor_id, livro_id, conteudo, criado_em)
                SELECT gen_random_uuid(), id, '30000000-0000-0000-0000-000000000001', 'Post de teste em lote', now()
                FROM usuario LIMIT 10
                """);
            jdbc.update(
                    """
                INSERT INTO curtida (id, post_id, usuario_id)
                SELECT gen_random_uuid(), p.id, u.id FROM post p CROSS JOIN usuario u
                WHERE p.conteudo = 'Post de teste em lote' AND u.id IN
                ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002')
                """);
            jdbc.update(
                    """
                INSERT INTO post (id, autor_id, livro_id, conteudo)
                VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000001',
                    '30000000-0000-0000-0000-000000000001', 'Sem curtidas')
                """);
            em.clear();
            stats.clear();
            var depois = service.buscarDetalhe(livroId);
            assertThat(stats.getPrepareStatementCount()).isEqualTo(consultas);
            assertThat(depois.posts().size()).isGreaterThan(antes.posts().size());
            assertThat(depois.posts())
                    .filteredOn(p -> p.texto().equals("Post de teste em lote"))
                    .isNotEmpty()
                    .allSatisfy(
                            p -> {
                                assertThat(p.curtidas()).isEqualTo(2);
                                assertThat(p.autor().nome()).isNotBlank();
                            });
            assertThat(depois.posts())
                    .filteredOn(p -> p.texto().equals("Sem curtidas"))
                    .hasSize(1)
                    .allSatisfy(p -> assertThat(p.curtidas()).isZero());
            for (var post : antes.posts()) assertThat(depois.posts()).contains(post);
        } finally {
            stats.setStatisticsEnabled(habilitado);
            stats.clear();
        }
    }

    @Test
    @DisplayName("curtidoPorMim vem falso quando o usuário atual não curtiu (#101)")
    void deve_devolver_curtidoPorMim_falso_por_padrao() {
        assertThat(mvc.get().uri(DOM_CASMURRO))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.posts[*].curtidoPorMim")
                .asList()
                .containsOnly(false);
    }

    @Test
    @Transactional
    @DisplayName("curtidoPorMim reflete a curtida do usuário atual (#101)")
    void deve_refletir_a_curtida_do_usuario_atual() {
        jdbc.update(
                "INSERT INTO curtida (id, post_id, usuario_id) VALUES (gen_random_uuid(),"
                        + " '72000000-0000-0000-0000-000000000001',"
                        + " '00000000-0000-0000-0000-000000000001')");

        assertThat(mvc.get().uri(DOM_CASMURRO))
                .hasStatusOk()
                .bodyJson()
                .extractingPath(
                        "$.posts[?(@.id=='72000000-0000-0000-0000-000000000001')].curtidoPorMim")
                .asList()
                .containsExactly(true);
    }

    @Test
    void deve_ler_detalhe_em_transacao_somente_leitura() {
        assertThat(mvc.get().uri(DOM_CASMURRO))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.livro.autor")
                .isEqualTo("Machado de Assis");
        assertThat(mvc.get().uri(DOM_CASMURRO))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.progresso.percentual")
                .isEqualTo(57);
        assertThat(mvc.get().uri(DOM_CASMURRO))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.progresso.paginaMaximaAlcancada")
                .isEqualTo(145);
    }

    @Test
    void deve_ignorar_usuario_informado_pelo_cliente() {
        assertThat(
                        mvc.get()
                                .uri(DOM_CASMURRO)
                                .param("userId", "00000000-0000-0000-0000-000000000002"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.progresso.paginaAtual")
                .isEqualTo(145);
    }

    @Test
    void deve_retornar_progresso_nulo_para_livro_fora_da_biblioteca() {
        assertThat(mvc.get().uri("/api/livros/30000000-0000-0000-0000-000000000013"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.progresso")
                .isNull();
    }

    @Test
    void deve_retornar_404_no_envelope_padrao() {
        assertThat(mvc.get().uri("/api/livros/30000000-0000-0000-0000-000000009999"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("LIVRO_NAO_ENCONTRADO");
    }

    @Test
    @Transactional
    @Sql(
            statements =
                    """
            INSERT INTO autor (id, nome) VALUES ('20000000-0000-0000-0000-000000009999', 'Coautor de teste');
            INSERT INTO livro_autor (livro_id, autor_id, ordem)
            VALUES ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000009999', 2);
            """)
    void deve_respeitar_a_ordem_dos_autores_da_relacao() {
        assertThat(mvc.get().uri(DOM_CASMURRO))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.livro.autor")
                .isEqualTo("Machado de Assis, Coautor de teste");
    }
}
