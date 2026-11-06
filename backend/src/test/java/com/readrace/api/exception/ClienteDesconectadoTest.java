package com.readrace.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import com.readrace.api.dto.response.ErroResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Garante que o cliente fechar a conexão antes da resposta (ex.: o app cancela a busca de livros
 * quando o termo muda) não vira linha ERROR no log. Cada linha ERROR conta para o alarme {@code
 * readrace-erros-na-api}, e esse caso não é falha do servidor (#255).
 */
@DisplayName("Cliente desconectado antes da resposta")
class ClienteDesconectadoTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ListAppender<ILoggingEvent> linhas = new ListAppender<>();

    @BeforeEach
    void capturarLog() {
        linhas.start();
        logger.addAppender(linhas);
    }

    @AfterEach
    void soltarLog() {
        logger.detachAppender(linhas);
    }

    @Test
    void broken_pipe_ao_escrever_json_nao_deve_gerar_erro_nem_resposta() {
        Exception ex =
                new HttpMessageNotWritableException(
                        "Could not write JSON",
                        new ClientAbortException(new IOException("Broken pipe")));

        ResponseEntity<ErroResponse> resposta = handler.tratarInesperado(ex);

        assertThat(resposta).isNull();
        assertThat(linhasDeErro()).isZero();
    }

    @Test
    void resposta_inutilizavel_nao_deve_gerar_erro_nem_resposta() {
        Exception ex = new AsyncRequestNotUsableException("ServletOutputStream failed to write");

        ResponseEntity<ErroResponse> resposta = handler.tratarInesperado(ex);

        assertThat(resposta).isNull();
        assertThat(linhasDeErro()).isZero();
    }

    @Test
    void excecao_inesperada_deve_continuar_como_erro_interno() {
        ResponseEntity<ErroResponse> resposta =
                handler.tratarInesperado(new IllegalStateException("falha de verdade"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().code()).isEqualTo(CodigoErro.INTERNAL_ERROR.name());
        assertThat(linhasDeErro()).isEqualTo(1);
    }

    private long linhasDeErro() {
        return linhas.list.stream().filter(linha -> linha.getLevel() == Level.ERROR).count();
    }
}
