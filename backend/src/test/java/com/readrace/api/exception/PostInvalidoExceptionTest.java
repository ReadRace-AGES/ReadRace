package com.readrace.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PostInvalidoExceptionTest {

    @Test
    void carrega_codigo_status_e_mensagem_de_post_invalido() {
        PostInvalidoException ex = new PostInvalidoException();

        assertThat(ex).isInstanceOf(ExcecaoDeNegocio.class);
        assertThat(ex.getCodigo()).isEqualTo(CodigoErro.POST_INVALIDO);
        assertThat(ex.getCodigo().status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getCodigo().status().value()).isEqualTo(400);
        assertThat(ex.getMessage())
                .isEqualTo("O post deve conter texto e ter no máximo 2.000 caracteres.")
                .isEqualTo(CodigoErro.POST_INVALIDO.mensagemPadrao());
    }
}
