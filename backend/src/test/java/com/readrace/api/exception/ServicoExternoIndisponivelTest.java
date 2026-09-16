package com.readrace.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Garante que uma falha de serviço externo (ex.: Google Books fora do ar) sai com um código próprio
 * — {@code EXTERNAL_SERVICE_UNAVAILABLE} — e não é confundida com erro interno ({@code
 * INTERNAL_ERROR}). É esse código que o mobile usa para decidir a mensagem ao usuário.
 */
@DisplayName("Serviço externo indisponível")
class ServicoExternoIndisponivelTest {

    @Test
    void excecao_deve_carregar_o_codigo_de_servico_externo() {
        ServicoExternoIndisponivelException ex =
                new ServicoExternoIndisponivelException("Google fora do ar");

        assertThat(ex.getCodigo()).isEqualTo(CodigoErro.EXTERNAL_SERVICE_UNAVAILABLE);
        assertThat(ex.getCodigo().status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void codigo_de_servico_externo_nao_deve_ser_erro_interno() {
        assertThat(CodigoErro.EXTERNAL_SERVICE_UNAVAILABLE).isNotEqualTo(CodigoErro.INTERNAL_ERROR);
    }

    @Test
    void para_status_deve_mapear_502_503_504_para_servico_externo() {
        assertThat(CodigoErro.paraStatus(HttpStatus.BAD_GATEWAY))
                .isEqualTo(CodigoErro.EXTERNAL_SERVICE_UNAVAILABLE);
        assertThat(CodigoErro.paraStatus(HttpStatus.SERVICE_UNAVAILABLE))
                .isEqualTo(CodigoErro.EXTERNAL_SERVICE_UNAVAILABLE);
        assertThat(CodigoErro.paraStatus(HttpStatus.GATEWAY_TIMEOUT))
                .isEqualTo(CodigoErro.EXTERNAL_SERVICE_UNAVAILABLE);
    }

    @Test
    void para_status_deve_manter_erro_interno_como_default() {
        assertThat(CodigoErro.paraStatus(HttpStatus.INTERNAL_SERVER_ERROR))
                .isEqualTo(CodigoErro.INTERNAL_ERROR);
    }
}
