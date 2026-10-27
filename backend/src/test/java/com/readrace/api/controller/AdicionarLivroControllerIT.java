
package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
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
 * Testes de integração de POST /api/biblioteca.
 *
 * <p>O catálogo local pode retornar ISBNs diferentes dos livros do seed.
 * Nesse caso, o sistema deve reutilizar o livro pelo título e primeiro autor.
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
    private static final String ISBN_HOBBIT = "9788595085800";

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
                "SELECT COUNT(*) FROM livro WHERE isbn = ?",
                Integer.class,
                isbn);
    }

    private UUID idDomCasmurroExistente() {
        return jdbc.queryForObject(
                "SELECT id FROM livro WHERE titulo = 'Dom Casmurro'",
                UUID.class);
    }

    private Integer contarItens(UUID livroId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM item_biblioteca"
                        + " WHERE usuario_id = ? AND livro_id = ?",
                Integer.class,
                USUARIO_ATUAL,
                livroId);
    }

    @Test
    @DisplayName("Dom Casmurro reutiliza livro do seed e adiciona como desejo")
    void deve_adicionar_livro_novo_como_desejo() {
        UUID livroExistente = idDomCasmurroExistente();

        assertThat(post("br001", "desejo"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("livroId", livroExistente.toString())
                .containsEntry("titulo", "Dom Casmurro")
                .containsEntry("status", "desejo");

        assertThat(contarLivrosPorIsbn(ISBN_BR001)).isZero();
        assertThat(contarItens(livroExistente)).isEqualTo(1);
    }

    @Test
    @DisplayName("trocar para lido depois de desejo não duplica livro nem item")
    void deve_trocar_status_sem_duplicar() {
        UUID livroExistente = idDomCasmurroExistente();

        assertThat(post("br001", "desejo")).hasStatus(HttpStatus.CREATED);

        assertThat(post("br001", "lido"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("livroId", livroExistente.toString())
                .containsEntry("status", "lido");

        assertThat(contarLivrosPorIsbn(ISBN_BR001)).isZero();
        assertThat(contarItens(livroExistente)).isEqualTo(1);
    }

    @Test
    @DisplayName("marcar como lido não concede XP")
    void marcar_como_lido_nao_mexe_em_registro_de_leitura() {
        UUID livroExistente = idDomCasmurroExistente();

        var usuarioAntes =
                jdbc.queryForMap(
                        "SELECT * FROM usuario WHERE id = ?",
                        USUARIO_ATUAL);

        Integer registrosAntes =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM registro_leitura r"
                                + " JOIN item_biblioteca i"
                                + " ON i.id = r.item_biblioteca_id"
                                + " WHERE i.usuario_id = ? AND i.livro_id = ?",
                        Integer.class,
                        USUARIO_ATUAL,
                        livroExistente);

        assertThat(post("br001", "lido")).hasStatus(HttpStatus.CREATED);

        assertThat(
                jdbc.queryForMap(
                        "SELECT * FROM usuario WHERE id = ?",
                        USUARIO_ATUAL))
                .isEqualTo(usuarioAntes);

        Integer registrosDepois =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM registro_leitura r"
                                + " JOIN item_biblioteca i"
                                + " ON i.id = r.item_biblioteca_id"
                                + " WHERE i.usuario_id = ? AND i.livro_id = ?",
                        Integer.class,
                        USUARIO_ATUAL,
                        livroExistente);

        assertThat(registrosDepois).isEqualTo(registrosAntes);
    }

    @Test
    @DisplayName("favoritar livro do catálogo retorna favorito verdadeiro")
    void deve_favoritar_livro_novo_e_entrar_como_desejo() {
        assertThat(post("br002", "favorito"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("favorito", true);
    }

    @Test
    @DisplayName("favoritar livro em leitura mantém status e página")
    void deve_favoritar_sem_mudar_status_nem_pagina_de_livro_em_leitura() {
        UUID livroExistente = idDomCasmurroExistente();

        assertThat(post("br001", "desejo")).hasStatus(HttpStatus.CREATED);

        jdbc.update(
                "UPDATE item_biblioteca"
                        + " SET status_leitura = 'lendo',"
                        + " pagina_atual = 50,"
                        + " pagina_maxima = GREATEST(pagina_maxima, 50)"
                        + " WHERE usuario_id = ? AND livro_id = ?",
                USUARIO_ATUAL,
                livroExistente);

        assertThat(post("br001", "favorito"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("status", "lendo")
                .containsEntry("favorito", true);

        Integer paginaAtual =
                jdbc.queryForObject(
                        "SELECT pagina_atual FROM item_biblioteca"
                                + " WHERE usuario_id = ? AND livro_id = ?",
                        Integer.class,
                        USUARIO_ATUAL,
                        livroExistente);

        assertThat(paginaAtual).isEqualTo(50);
    }

    @Test
    @DisplayName("ISBN existente tem prioridade sobre título e autor")
    void nao_deve_duplicar_livro_quando_isbn_ja_existe() {
        UUID livroExistente = UUID.randomUUID();

        jdbc.update(
                "INSERT INTO livro (id, isbn, titulo, total_paginas)"
                        + " VALUES (?, ?, 'Dom Casmurro (edição existente)', 300)",
                livroExistente,
                ISBN_BR001);

        assertThat(post("br001", "desejo"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.livroId")
                .isEqualTo(livroExistente.toString());

        assertThat(contarLivrosPorIsbn(ISBN_BR001)).isEqualTo(1);
        assertThat(contarItens(livroExistente)).isEqualTo(1);
    }

    @Test
    @DisplayName("lista inválida devolve 400 LISTA_INVALIDA")
    void deve_devolver_400_para_lista_invalida() {
        assertThat(post("br001", "lendo"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("LISTA_INVALIDA");
    }

    @Test
    @DisplayName("volume inexistente devolve 404 LIVRO_NAO_ENCONTRADO")
    void deve_devolver_404_para_volume_inexistente() {
        assertThat(post("nao-existe-no-catalogo", "desejo"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("LIVRO_NAO_ENCONTRADO");
    }

    @Test
    @DisplayName("O Hobbit reutiliza livro existente e preserva seus metadados")
    void deve_reutilizar_hobbit_sem_alterar_capa_ou_paginas() {
        Map<String, Object> livroAntes = jdbc.queryForMap(
                "SELECT id, isbn, titulo, total_paginas, capa_url"
                        + " FROM livro WHERE titulo = 'O Hobbit'");

        UUID livroId = (UUID) livroAntes.get("id");
        Object isbnOriginal = livroAntes.get("isbn");
        Object paginasOriginais = livroAntes.get("total_paginas");
        Object capaOriginal = livroAntes.get("capa_url");

        Integer quantidadeAntes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM livro",
                Integer.class);

        assertThat(post("fan003", "desejo"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("livroId", livroId.toString())
                .containsEntry("titulo", "O Hobbit")
                .containsEntry("status", "desejo");

        Map<String, Object> livroDepois = jdbc.queryForMap(
                "SELECT id, isbn, titulo, total_paginas, capa_url"
                        + " FROM livro WHERE id = ?",
                livroId);

        Integer quantidadeDepois = jdbc.queryForObject(
                "SELECT COUNT(*) FROM livro",
                Integer.class);

        assertThat(livroDepois.get("id")).isEqualTo(livroId);
        assertThat(livroDepois.get("isbn")).isEqualTo(isbnOriginal);
        assertThat(livroDepois.get("total_paginas")).isEqualTo(paginasOriginais);
        assertThat(livroDepois.get("capa_url")).isEqualTo(capaOriginal);

        assertThat(quantidadeDepois).isEqualTo(quantidadeAntes);
        assertThat(contarItens(livroId)).isEqualTo(1);

        if (!ISBN_HOBBIT.equals(isbnOriginal)) {
            assertThat(contarLivrosPorIsbn(ISBN_HOBBIT)).isZero();
        }
    }
}
