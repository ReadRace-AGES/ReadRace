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
import org.springframework.web.server.ResponseStatusException;

import com.readrace.api.TestcontainersConfiguration;

@Import({TestcontainersConfiguration.class, FormatoDeErroIT.ControllerQueExplode.class})
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Formato padrão de erro")
class FormatoDeErroIT {
    @Autowired private MockMvcTester mvc;

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

    @Test
    void deve_devolver_route_not_found_em_json_quando_a_rota_nao_existir() {
        assertThat(mvc.get().uri("/api/rota-que-nao-existe"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("ROUTE_NOT_FOUND");
    }

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

    @Test
    void deve_devolver_internal_error_quando_estourar_excecao_nao_prevista() {
        assertThat(mvc.get().uri("/teste/explode"))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void nao_deve_vazar_a_mensagem_interna_da_excecao_no_corpo_do_500() {
        assertThat(mvc.get().uri("/teste/explode"))
                .bodyJson()
                .extractingPath("$.message")
                .asString()
                .doesNotContain("senha do banco");
    }

    @Test
    void deve_respeitar_o_status_da_response_status_exception() {
        assertThat(mvc.get().uri("/teste/response-status"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    void deve_usar_o_motivo_da_response_status_exception_como_message() {
        assertThat(mvc.get().uri("/teste/response-status"))
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("Filtro de busca ausente");
    }

    @Test
    void nunca_deve_devolver_html_em_resposta_de_erro() {
        assertThat(mvc.get().uri("/api/rota-que-nao-existe"))
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON);
    }

    @Test
    void deve_devolver_json_tambem_na_rota_interna_de_erro() {
        assertThat(mvc.get().uri("/error"))
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("INTERNAL_ERROR");
    }

    @RestController
    static class ControllerQueExplode {
        @GetMapping("/teste/explode")
        String explode() {
            throw new IllegalStateException("detalhe interno com a senha do banco");
        }

        @GetMapping("/teste/response-status")
        String responseStatus() {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro de busca ausente");
        }
    }
}
