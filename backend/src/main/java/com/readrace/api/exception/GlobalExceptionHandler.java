package com.readrace.api.exception;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.readrace.api.dto.response.ErroResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ExcecaoDeNegocio.class)
    public ResponseEntity<ErroResponse> tratarNegocio(ExcecaoDeNegocio ex) {
        return resposta(ex.getCodigo(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex) {
        String mensagem =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(this::descrever)
                        .collect(Collectors.joining("; "));

        return resposta(CodigoErro.VALIDATION_ERROR, mensagem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoIlegivel(HttpMessageNotReadableException ex) {
        log.debug("Corpo da requisição ilegível", ex);

        return resposta(CodigoErro.MALFORMED_REQUEST, "Corpo da requisição inválido ou ausente.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return resposta(
                CodigoErro.MALFORMED_REQUEST,
                "O valor de '%s' não está no formato esperado.".formatted(ex.getName()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> tratarParametroAusente(
            MissingServletRequestParameterException ex) {
        return resposta(
                CodigoErro.MALFORMED_REQUEST,
                "O parâmetro '%s' é obrigatório.".formatted(ex.getParameterName()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> tratarRotaInexistente(NoResourceFoundException ex) {
        return resposta(
                CodigoErro.ROUTE_NOT_FOUND,
                "Rota não encontrada: %s".formatted(ex.getResourcePath()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarVerboInvalido(
            HttpRequestMethodNotSupportedException ex) {
        return resposta(
                CodigoErro.METHOD_NOT_ALLOWED,
                "O método %s não é aceito nesta rota.".formatted(ex.getMethod()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarMidiaInvalida(HttpMediaTypeNotSupportedException ex) {
        return resposta(CodigoErro.UNSUPPORTED_MEDIA_TYPE, "Envie o corpo como application/json.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarInesperado(Exception ex) {
        log.error("Exceção não tratada chegou ao handler global", ex);

        return resposta(CodigoErro.INTERNAL_ERROR, CodigoErro.INTERNAL_ERROR.mensagemPadrao());
    }

    private ResponseEntity<ErroResponse> resposta(CodigoErro codigo, String mensagem) {
        return ResponseEntity.status(codigo.status()).body(ErroResponse.de(codigo, mensagem));
    }

    private String descrever(FieldError erro) {
        return "%s: %s".formatted(erro.getField(), erro.getDefaultMessage());
    }
}
