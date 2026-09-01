package com.readrace.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.TestcontainersConfiguration;

/**
 * O contrato de erro da issue #10: TODA resposta 4xx/5xx é um JSON {"code","message"}.
 *
 * <p>Um teste por caminho de erro. Se algum deles voltar a devolver HTML, corpo vazio ou o formato
 * antigo de ProblemDetail, quebra aqui — que é o ponto: o critério de validação #3 exige "never
 * stack traces or HTML".
 */
@Import({TestcontainersConfiguration.class, FormatoDeErroIT.ControllerQueExplode.class})
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Formato padrão de erro")
class FormatoDeErroIT {

    @Autowired private MockMvcTester mvc;

    // ==================== 404 de negócio ====================

    @Test
    void deve_devolver_resource_not_found_quando_o_id_nao_existir() {
        assertThat(mvc.get().uri("/api/exemplos/{id}", 999L))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void deve_usar_a_mensagem_da_excecao_de_negocio_como_message() {
        assertThat(mvc.get().uri("/api/exemplos/{id}", 999L))
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("Exemplo 999 não encontrado");
    }

    // ==================== 404 de rota ====================

    @Test
    void deve_devolver_route_not_found_em_json_quando_a_rota_nao_existir() {
        assertThat(mvc.get().uri("/api/rota-que-nao-existe"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("ROUTE_NOT_FOUND");
    }

    // ==================== 400 ====================

    @Test
    void deve_devolver_validation_error_apontando_o_campo_reprovado() {
        assertThat(
                        mvc.post()
                                .uri("/api/exemplos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nome\":\"\",\"descricao\":\"sem nome\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("nome: informe o nome");
    }

    @Test
    void deve_devolver_malformed_request_quando_o_json_estiver_quebrado() {
        assertThat(
                        mvc.post()
                                .uri("/api/exemplos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nome\":"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    void deve_devolver_malformed_request_quando_o_id_da_url_nao_for_numero() {
        assertThat(mvc.get().uri("/api/exemplos/abc"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("MALFORMED_REQUEST");
    }

    // ==================== 405 e 415 ====================

    @Test
    void deve_devolver_method_not_allowed_quando_o_verbo_nao_existir_na_rota() {
        assertThat(mvc.patch().uri("/api/exemplos"))
                .hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    void deve_devolver_unsupported_media_type_quando_o_corpo_nao_for_json() {
        assertThat(
                        mvc.post()
                                .uri("/api/exemplos")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("isto não é json"))
                .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("UNSUPPORTED_MEDIA_TYPE");
    }

    // ==================== 500 ====================

    @Test
    void deve_devolver_internal_error_quando_estourar_excecao_nao_prevista() {
        assertThat(mvc.get().uri("/teste/explode"))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("INTERNAL_ERROR");
    }

    /** O detalhe técnico vai para o log, nunca para o cliente. */
    @Test
    void nao_deve_vazar_a_mensagem_interna_da_excecao_no_corpo_do_500() {
        assertThat(mvc.get().uri("/teste/explode"))
                .bodyJson()
                .extractingPath("$.message")
                .asString()
                .doesNotContain("senha do banco");
    }

    // ==================== o corpo é sempre JSON ====================

    @Test
    void nunca_deve_devolver_html_em_resposta_de_erro() {
        assertThat(mvc.get().uri("/api/rota-que-nao-existe"))
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);
    }

    /**
     * A rota /error é o despachante interno do Tomcat: erro estourado num filtro (antes do
     * DispatcherServlet) cai aqui, fora do alcance do @RestControllerAdvice. Sem tratamento próprio
     * ela devolve a whitelabel page, em HTML.
     */
    @Test
    void deve_devolver_json_tambem_na_rota_interna_de_erro() {
        assertThat(mvc.get().uri("/error"))
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("INTERNAL_ERROR");
    }

    /**
     * Endpoint que só existe durante o teste: precisa de uma exceção não mapeada para provar o
     * fallback de 500. Fica aqui, e não no código de produção, de propósito.
     */
    @RestController
    static class ControllerQueExplode {

        @GetMapping("/teste/explode")
        String explode() {
            throw new IllegalStateException("detalhe interno com a senha do banco");
        }
    }
}
