package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.dto.GoogleBookVolume;
import com.readrace.api.dto.IndustryIdentifier;
import com.readrace.api.dto.VolumeInfo;
import com.readrace.api.service.BookSearchPort;

/**
 * Título do catálogo fora do padrão em {@code POST /api/biblioteca} (#224). O catálogo é mockado: o
 * catálogo local de teste não tem título longo nem volume sem título. Fica numa classe à parte
 * porque o mock da porta valeria para todos os testes de {@link AdicionarLivroControllerIT}.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("POST /api/biblioteca com título fora do padrão")
class AdicionarLivroTituloIT {

    private static final String ISBN = "9786586490999";

    @Autowired private MockMvcTester mvc;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private BookSearchPort bookSearchPort;

    private void catalogoDevolve(String titulo) {
        VolumeInfo info =
                new VolumeInfo(
                        titulo,
                        null,
                        List.of("Autor de teste"),
                        null,
                        null,
                        null,
                        List.of(new IndustryIdentifier("ISBN_13", ISBN)),
                        200,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        given(bookSearchPort.getById("vol-titulo"))
                .willReturn(Optional.of(GoogleBookVolume.of("vol-titulo", info)));
    }

    private MockMvcTester.MockMvcRequestBuilder adicionar() {
        return mvc.post()
                .uri("/api/biblioteca")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"volumeId\":\"vol-titulo\",\"lista\":\"desejo\"}");
    }

    @Test
    @DisplayName("título com mais de 255 caracteres é cortado e o livro é criado (201)")
    void deve_cortar_titulo_longo_em_vez_de_dar_500() {
        catalogoDevolve("t".repeat(400));

        assertThat(adicionar()).hasStatus(HttpStatus.CREATED);

        assertThat(
                        jdbc.queryForObject(
                                "SELECT length(titulo) FROM livro WHERE isbn = ?",
                                Integer.class,
                                ISBN))
                .isEqualTo(255);
    }

    @Test
    @DisplayName("volume sem título responde LIVRO_SEM_ISBN_OU_PAGINAS e não cria o livro")
    void deve_recusar_volume_sem_titulo() {
        catalogoDevolve(null);

        assertThat(adicionar())
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("LIVRO_SEM_ISBN_OU_PAGINAS");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM livro WHERE isbn = ?", Integer.class, ISBN))
                .isEqualTo(0);
    }
}
