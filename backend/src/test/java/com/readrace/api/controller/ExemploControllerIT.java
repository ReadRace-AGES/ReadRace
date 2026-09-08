package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.model.Exemplo;
import com.readrace.api.repository.ExemploRepository;

/**
 * Teste de integração: contexto Spring completo, HTTP de verdade pelo MockMvc e PostgreSQL real via
 * Testcontainers — sem H2 e sem repositório mockado, como manda o padrão do projeto.
 *
 * <p>Sufixo {@code IT} para separar do teste unitário na hora de ler o relatório do CI. Requer
 * Docker rodando.
 *
 * <p>Este é o formato padrão de teste de endpoint do projeto. Copie daqui.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GET/POST/PUT/DELETE /api/exemplos")
class ExemploControllerIT {

    @Autowired private MockMvcTester mvc;

    @Autowired private ExemploRepository exemploRepository;

    @BeforeEach
    void limpar_a_tabela() {
        exemploRepository.deleteAll();
    }

    @Test
    void deve_criar_e_recuperar_o_exemplo_pelo_id() {
        assertThat(
                        mvc.post()
                                .uri("/api/exemplos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"nome":"Dom Casmurro","descricao":"Machado de Assis"}"""))
                .hasStatus(HttpStatus.CREATED);

        Long id = exemploRepository.findAll().getFirst().getId();

        assertThat(mvc.get().uri("/api/exemplos/{id}", id))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.nome")
                .isEqualTo("Dom Casmurro");
    }

    @Test
    void deve_devolver_404_no_formato_padrao_quando_o_id_nao_existir() {
        assertThat(mvc.get().uri("/api/exemplos/{id}", 999L))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    /** A validação do @NotBlank tem que virar 400 apontando o campo, não 500. */
    @Test
    void deve_devolver_400_apontando_o_campo_quando_o_nome_for_vazio() {
        assertThat(
                        mvc.post()
                                .uri("/api/exemplos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"nome":"","descricao":"sem nome"}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("nome: informe o nome");
    }

    /**
     * Prova o dirty checking contra o banco real: o service não chama save, e mesmo assim o UPDATE
     * acontece no commit da transação.
     */
    @Test
    void deve_persistir_a_alteracao_ao_atualizar_mesmo_sem_save() {
        Exemplo existente = exemploRepository.save(new Exemplo("nome antigo", "descrição antiga"));

        assertThat(
                        mvc.put()
                                .uri("/api/exemplos/{id}", existente.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"nome":"nome novo","descricao":"descrição nova"}"""))
                .hasStatusOk();

        assertThat(exemploRepository.findById(existente.getId()))
                .get()
                .extracting(Exemplo::getNome, Exemplo::getDescricao)
                .containsExactly("nome novo", "descrição nova");
    }

    @Test
    void deve_devolver_204_e_remover_do_banco_ao_excluir() {
        Exemplo existente = exemploRepository.save(new Exemplo("descartável", null));

        assertThat(mvc.delete().uri("/api/exemplos/{id}", existente.getId()))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(exemploRepository.findById(existente.getId())).isEmpty();
    }

    @Test
    void deve_devolver_404_ao_excluir_id_inexistente() {
        assertThat(mvc.delete().uri("/api/exemplos/{id}", 999L)).hasStatus(HttpStatus.NOT_FOUND);
    }

    /**
     * Sem profile ativo, {@code readrace.cors.allowed-origins} é vazio e nenhuma origem é liberada.
     * Se alguém voltar a fixar {@code "*"} no código, este teste quebra.
     */
    @Test
    void nao_deve_liberar_cors_quando_nenhuma_origem_estiver_configurada() {
        assertThat(mvc.get().uri("/api/exemplos").header("Origin", "https://qualquer.example"))
                .hasStatusOk()
                .doesNotContainHeader("Access-Control-Allow-Origin");
    }
}
