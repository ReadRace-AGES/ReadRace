package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.model.DesafioAmigo;
import com.readrace.api.model.ProgressoDesafio;
import com.readrace.api.repository.DesafioAmigoRepository;
import com.readrace.api.repository.ProgressoDesafioRepository;
import com.readrace.api.repository.SeguirRepository;
import com.readrace.api.service.UsuarioAtualDeSeed;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("GET/POST /api/desafios")
class DesafioControllerIT {

    private static final UUID ANA_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID LIVRO_ID = UUID.fromString("30000000-0000-0000-0000-000000000004");
    private static final UUID ID_INEXISTENTE =
            UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    private static final UUID SEGUIMENTO_ANA_DANIEL_ID =
            UUID.fromString("80000000-0000-0000-0000-000000000002");
    private static final UUID SEGUIMENTO_DANIEL_ANA_ID =
            UUID.fromString("80000000-0000-0000-0000-000000000001");
    private static final String DESAFIO_RECUSADO_ID = "90000000-0000-0000-0000-000000000001";

    @Autowired private MockMvcTester mvc;

    @Autowired private DesafioAmigoRepository desafioRepository;

    @Autowired private ProgressoDesafioRepository progressoRepository;

    @Autowired private SeguirRepository seguirRepository;

    @Autowired private UsuarioAtualDeSeed usuarioAtual;

    @Autowired private EntityManager entityManager;

    @Test
    void deve_criar_desafio_por_paginas_com_dois_progressos_zerados() {
        long quantidadeDesafiosAntes = desafioRepository.count();
        long quantidadeProgressosAntes = progressoRepository.count();

        assertThat(
                        mvc.post()
                                .uri("/api/desafios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "oponenteId": "%s",
                                            "tipoMeta": "paginas",
                                            "meta": 150,
                                            "prazoDias": 7
                                        }
                                        """
                                                .formatted(ANA_ID)))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("pendente");

        assertThat(desafioRepository.count()).isEqualTo(quantidadeDesafiosAntes + 1);
        assertThat(progressoRepository.count()).isEqualTo(quantidadeProgressosAntes + 2);
    }

    @Test
    void deve_listar_desafios_com_os_status_esperados() {
        assertThat(mvc.get().uri("/api/desafios"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.desafios[*].status")
                .asArray()
                .containsExactlyInAnyOrder(
                        "pendente", "em_andamento", "concluido_ganho", "concluido_perdido");
    }

    @Test
    @Sql(
            statements =
                    "UPDATE desafio_amigo SET status = 'recusado' WHERE id = '"
                            + DESAFIO_RECUSADO_ID
                            + "'")
    void nao_deve_listar_desafios_recusados() {
        MvcTestResult resultado = mvc.get().uri("/api/desafios").exchange();

        assertThat(resultado)
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.desafios[*].id")
                .asArray()
                .hasSize(3)
                .doesNotContain(DESAFIO_RECUSADO_ID);
        assertThat(resultado)
                .bodyJson()
                .extractingPath("$.desafios[*].status")
                .asArray()
                .doesNotContain("recusado");
    }

    @Test
    void deve_devolver_404_quando_o_desafio_nao_existir() {
        assertThat(mvc.get().uri("/api/desafios/{id}", ID_INEXISTENTE))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("DESAFIO_NAO_ENCONTRADO");
    }

    @Test
    void deve_buscar_oponentes_por_parte_do_username() {
        assertThat(mvc.get().uri("/api/desafios/oponentes").param("q", "ana"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.oponentes[*].username")
                .asArray()
                .containsExactlyInAnyOrder("anasilva", "marianacosta");
    }

    @Test
    void deve_criar_desafio_por_livro_com_dois_progressos_zerados() {
        long quantidadeDesafiosAntes = desafioRepository.count();
        long quantidadeProgressosAntes = progressoRepository.count();

        MvcTestResult resultado =
                mvc.post()
                        .uri("/api/desafios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                    "oponenteId": "%s",
                                    "tipoMeta": "livro",
                                    "livroId": "%s",
                                    "prazoDias": 7
                                }
                                """
                                        .formatted(ANA_ID, LIVRO_ID))
                        .exchange();

        assertThat(resultado)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.tipoMeta")
                .isEqualTo("livro");
        assertThat(resultado)
                .bodyJson()
                .extractingPath("$.livro.titulo")
                .isEqualTo("A Metamorfose");
        assertThat(resultado)
                .bodyJson()
                .extractingPath("$.livro.id")
                .isEqualTo(LIVRO_ID.toString());
        assertThat(resultado).bodyJson().extractingPath("$.livro.autor").isEqualTo("Franz Kafka");
        assertThat(resultado)
                .bodyJson()
                .extractingPath("$.livro.capaUrl")
                .isEqualTo("https://covers.openlibrary.org/b/isbn/9780805209990-L.jpg");
        assertThat(resultado).bodyJson().extractingPath("$.status").isEqualTo("pendente");
        assertThat(resultado).bodyJson().extractingPath("$.progresso.voce").isEqualTo(0);
        assertThat(resultado).bodyJson().extractingPath("$.progresso.oponente").isEqualTo(0);
        assertThat(resultado).bodyJson().extractingPath("$").asMap().doesNotContainKey("meta");

        entityManager.flush();
        entityManager.clear();

        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        assertThat(resultado)
                .bodyJson()
                .extractingPath("$.id")
                .asString()
                .satisfies(
                        id -> {
                            UUID desafioId = UUID.fromString(id);
                            DesafioAmigo desafio =
                                    desafioRepository.findById(desafioId).orElseThrow();

                            assertThat(desafio.getCriador().getId()).isEqualTo(usuarioId);
                            assertThat(desafio.getCriador().getNomeUsuario())
                                    .isEqualTo("danielribeiro");
                            assertThat(desafio.getOponente().getId()).isEqualTo(ANA_ID);
                            assertThat(desafio.getLivro().getId()).isEqualTo(LIVRO_ID);
                            assertThat(desafio.getLivro().getTitulo()).isEqualTo("A Metamorfose");
                            assertThat(desafio.getMetaValor()).isEqualTo(1);
                            assertThat(desafio.getTitulo()).isEqualTo("Desafio de leitura");
                            assertThat(desafio.getDescricao())
                                    .isEqualTo("Quem termina A Metamorfose primeiro");

                            List<ProgressoDesafio> progressos =
                                    progressoRepository.buscarPorDesafios(List.of(desafioId));

                            assertThat(progressos)
                                    .hasSize(2)
                                    .allSatisfy(
                                            progresso ->
                                                    assertThat(progresso.getDesafio().getId())
                                                            .isEqualTo(desafioId))
                                    .extracting(
                                            progresso -> progresso.getUsuario().getId(),
                                            ProgressoDesafio::getValorAtual)
                                    .containsExactlyInAnyOrder(
                                            tuple(usuarioId, 0), tuple(ANA_ID, 0));
                        });

        assertThat(desafioRepository.count()).isEqualTo(quantidadeDesafiosAntes + 1);
        assertThat(progressoRepository.count()).isEqualTo(quantidadeProgressosAntes + 2);
    }

    @Test
    void deve_rejeitar_meta_invalida_sem_criar_desafio_ou_progressos() {
        long quantidadeDesafiosAntes = desafioRepository.count();
        long quantidadeProgressosAntes = progressoRepository.count();

        assertThat(
                        mvc.post()
                                .uri("/api/desafios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "oponenteId": "%s",
                                            "tipoMeta": "paginas",
                                            "meta": 3,
                                            "prazoDias": 7
                                        }
                                        """
                                                .formatted(ANA_ID)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("META_INVALIDA");

        assertThat(desafioRepository.count()).isEqualTo(quantidadeDesafiosAntes);
        assertThat(progressoRepository.count()).isEqualTo(quantidadeProgressosAntes);
    }

    @Test
    void deve_rejeitar_desafio_por_livro_sem_livro_id() {
        assertThat(
                        mvc.post()
                                .uri("/api/desafios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "oponenteId": "%s",
                                            "tipoMeta": "livro",
                                            "prazoDias": 7
                                        }
                                        """
                                                .formatted(ANA_ID)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("LIVRO_OBRIGATORIO");
    }

    @Test
    void deve_rejeitar_prazo_zero() {
        assertThat(
                        mvc.post()
                                .uri("/api/desafios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "oponenteId": "%s",
                                            "tipoMeta": "paginas",
                                            "meta": 150,
                                            "prazoDias": 0
                                        }
                                        """
                                                .formatted(ANA_ID)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("PRAZO_INVALIDO");
    }

    @Test
    void deve_devolver_404_quando_o_oponente_nao_existir() {
        assertThat(
                        mvc.post()
                                .uri("/api/desafios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "oponenteId": "%s",
                                            "tipoMeta": "paginas",
                                            "meta": 150,
                                            "prazoDias": 7
                                        }
                                        """
                                                .formatted(ID_INEXISTENTE)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("OPONENTE_NAO_ENCONTRADO");
    }

    @Test
    void deve_devolver_404_quando_o_livro_nao_existir() {
        assertThat(
                        mvc.post()
                                .uri("/api/desafios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "oponenteId": "%s",
                                            "tipoMeta": "livro",
                                            "livroId": "%s",
                                            "prazoDias": 7
                                        }
                                        """
                                                .formatted(ANA_ID, ID_INEXISTENTE)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("LIVRO_NAO_ENCONTRADO");
    }

    @Test
    @Sql(
            statements =
                    """
                    UPDATE usuario
                    SET excluido_em = NOW()
                    WHERE id = '00000000-0000-0000-0000-000000000002';
                    """)
    void deve_devolver_404_quando_o_oponente_estiver_excluido() {
        assertThat(
                        mvc.post()
                                .uri("/api/desafios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "oponenteId": "%s",
                                            "tipoMeta": "paginas",
                                            "meta": 150,
                                            "prazoDias": 7
                                        }
                                        """
                                                .formatted(ANA_ID)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("OPONENTE_NAO_ENCONTRADO");
    }

    @Test
    void deve_rejeitar_oponente_sem_seguimento_reciproco() {
        assertThat(seguirRepository.existsById(SEGUIMENTO_ANA_DANIEL_ID)).isTrue();
        seguirRepository.deleteById(SEGUIMENTO_ANA_DANIEL_ID);
        seguirRepository.flush();

        assertThat(seguirRepository.existsById(SEGUIMENTO_ANA_DANIEL_ID)).isFalse();
        assertThat(seguirRepository.existsById(SEGUIMENTO_DANIEL_ANA_ID)).isTrue();

        assertThat(
                        mvc.post()
                                .uri("/api/desafios")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                            "oponenteId": "%s",
                                            "tipoMeta": "paginas",
                                            "meta": 150,
                                            "prazoDias": 7
                                        }
                                        """
                                                .formatted(ANA_ID)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("OPONENTE_NAO_E_AMIGO");
    }
}
