package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;

/**
 * Teste de integração de {@code POST /api/biblioteca} (#155).
 *
 * <p>Roda SEM profile "prod", então o catálogo é o {@code LocalBooksAdapter} a partir do {@code
 * books-seed.json} real. {@code br001} (Dom Casmurro, ISBN 9786586490077) nunca está pré-cadastrado
 * no seed do banco (V4), que usa outro ISBN para o mesmo título — por isso serve para testar a
 * criação de um livro novo em todos os testes, sem interferir no restante do seed.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("POST /api/biblioteca")
class AdicionarLivroControllerIT {

    private static final String URL = "/api/biblioteca";
    private static final UUID USUARIO_ATUAL =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String ISBN_BR001 = "9786586490077";

    @Autowired private MockMvcTester mvc;
    @Autowired private JdbcTemplate jdbc;

    private static String corpo(String volumeId, String lista) {
        return "{\"volumeId\":\"%s\",\"lista\":\"%s\"}".formatted(volumeId, lista);
    }

    private org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder post(
            String volumeId, String lista) {
        return mvc.post()
                .uri(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(volumeId, lista));
    }

    private Integer contarLivrosPorIsbn(String isbn) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM livro WHERE isbn = ?", Integer.class, isbn);
    }

    @Test
    @DisplayName("livro novo como desejo: cria o livro (pelo ISBN) e o item, sem XP")
    void deve_adicionar_livro_novo_como_desejo() {
        assertThat(post("br001", "desejo"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("titulo", "Dom Casmurro")
                .containsEntry("status", "desejo")
                .containsEntry("favorito", false);

        assertThat(contarLivrosPorIsbn(ISBN_BR001)).isEqualTo(1);

        Integer itens =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM item_biblioteca i JOIN livro l ON l.id = i.livro_id"
                                + " WHERE i.usuario_id = ? AND l.isbn = ? AND i.status_leitura ="
                                + " 'desejo' AND i.favorito = false",
                        Integer.class,
                        USUARIO_ATUAL,
                        ISBN_BR001);
        assertThat(itens).isEqualTo(1);
    }

    @Test
    @DisplayName("trocar para lido depois de desejo troca o status, sem duplicar livro nem item")
    void deve_trocar_status_sem_duplicar() {
        assertThat(post("br001", "desejo")).hasStatus(HttpStatus.CREATED);
        assertThat(post("br001", "lido"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("lido");

        assertThat(contarLivrosPorIsbn(ISBN_BR001)).isEqualTo(1);
        Integer totalItens =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM item_biblioteca i JOIN livro l ON l.id = i.livro_id"
                                + " WHERE i.usuario_id = ? AND l.isbn = ?",
                        Integer.class,
                        USUARIO_ATUAL,
                        ISBN_BR001);
        assertThat(totalItens).isEqualTo(1);
    }

    @Test
    @DisplayName("marcar como lido não concede XP: tela de progresso não é tocada")
    void marcar_como_lido_nao_mexe_em_registro_de_leitura() {
        assertThat(post("br001", "lido")).hasStatus(HttpStatus.CREATED);

        Integer registros =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM registro_leitura r"
                                + " JOIN item_biblioteca i ON i.id = r.item_biblioteca_id"
                                + " JOIN livro l ON l.id = i.livro_id"
                                + " WHERE l.isbn = ?",
                        Integer.class,
                        ISBN_BR001);
        assertThat(registros).isZero();
    }

    @Test
    @DisplayName("favoritar livro novo também entra como desejo")
    void deve_favoritar_livro_novo_e_entrar_como_desejo() {
        assertThat(post("br002", "favorito"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("status", "desejo")
                .containsEntry("favorito", true);
    }

    @Test
    @DisplayName("favoritar livro que já está lendo mantém status e página, só marca a flag")
    void deve_favoritar_sem_mudar_status_nem_pagina_de_livro_em_leitura() {
        assertThat(post("br001", "desejo")).hasStatus(HttpStatus.CREATED);
        jdbc.update(
                "UPDATE item_biblioteca SET status_leitura = 'lendo', pagina_atual = 50,"
                        + " pagina_maxima = 50 WHERE usuario_id = ? AND livro_id = (SELECT id FROM"
                        + " livro WHERE isbn = ?)",
                USUARIO_ATUAL,
                ISBN_BR001);

        assertThat(post("br001", "favorito"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("status", "lendo")
                .containsEntry("favorito", true);

        Integer paginaAtual =
                jdbc.queryForObject(
                        "SELECT pagina_atual FROM item_biblioteca i JOIN livro l ON l.id ="
                                + " i.livro_id WHERE i.usuario_id = ? AND l.isbn = ?",
                        Integer.class,
                        USUARIO_ATUAL,
                        ISBN_BR001);
        assertThat(paginaAtual).isEqualTo(50);
    }

    @Test
    @DisplayName("ISBN já cadastrado por outro fluxo: reaproveita o livro existente, sem duplicar")
    void nao_deve_duplicar_livro_quando_isbn_ja_existe() {
        UUID livroExistente = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO livro (id, isbn, titulo, total_paginas) VALUES (?, ?, 'Dom Casmurro"
                        + " (edição existente)', 300)",
                livroExistente,
                ISBN_BR001);

        assertThat(post("br001", "desejo")).hasStatus(HttpStatus.CREATED);

        assertThat(contarLivrosPorIsbn(ISBN_BR001)).isEqualTo(1);
        UUID livroDoItem =
                jdbc.queryForObject(
                        "SELECT livro_id FROM item_biblioteca WHERE usuario_id = ? AND livro_id ="
                                + " (SELECT id FROM livro WHERE isbn = ?)",
                        UUID.class,
                        USUARIO_ATUAL,
                        ISBN_BR001);
        assertThat(livroDoItem).isEqualTo(livroExistente);
    }

    @Test
    @DisplayName("lista fora de lido/desejo/favorito devolve 400 LISTA_INVALIDA")
    void deve_devolver_400_para_lista_invalida() {
        assertThat(post("br001", "lendo"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("LISTA_INVALIDA");
    }

    @Test
    @DisplayName("volumeId que o catálogo não conhece devolve 404 LIVRO_NAO_ENCONTRADO")
    void deve_devolver_404_para_volume_inexistente() {
        assertThat(post("nao-existe-no-catalogo", "desejo"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("LIVRO_NAO_ENCONTRADO");
    }
}
