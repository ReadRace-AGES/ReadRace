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

/**
 * O único lugar da aplicação que transforma exceção em resposta HTTP.
 *
 * <p>Consequência prática: <b>não existe try/catch em controller nem em service</b> para montar
 * resposta de erro. O service lança, isto aqui traduz. Se um caminho de erro novo aparecer, ele é
 * adicionado nesta classe — não espalhado.
 *
 * <p>Cobre tanto as exceções de domínio quanto as do próprio Spring MVC (rota inexistente, verbo
 * errado, JSON quebrado). Sem isso, o Spring devolveria o formato dele — ou corpo vazio — e o
 * critério de validação #3 da issue #10 ("nunca stack trace ou HTML") não seria verdade.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ==================== Domínio ====================

    /**
     * Um handler para TODAS as exceções de negócio: cada uma já sabe o próprio código. Criar uma
     * exceção nova não exige tocar nesta classe.
     */
    @ExceptionHandler(ExcecaoDeNegocio.class)
    public ResponseEntity<ErroResponse> tratarNegocio(ExcecaoDeNegocio ex) {
        return resposta(ex.getCodigo(), ex.getMessage());
    }

    // ==================== Requisição malformada ====================

    /**
     * Falha de @Valid. A message junta campo e motivo porque o contrato só tem duas chaves — o
     * mobile mostra o texto direto, sem precisar interpretar um mapa.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex) {
        String mensagem =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(this::descrever)
                        .collect(Collectors.joining("; "));

        return resposta(CodigoErro.VALIDATION_ERROR, mensagem);
    }

    /** JSON sintaticamente quebrado, corpo ausente ou tipo incompatível dentro do JSON. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoIlegivel(HttpMessageNotReadableException ex) {
        // A mensagem original expõe nome de classe Java e posição no parser: fica no log.
        log.debug("Corpo da requisição ilegível", ex);

        return resposta(CodigoErro.MALFORMED_REQUEST, "Corpo da requisição inválido ou ausente.");
    }

    /** Path variable ou query param com tipo errado: {@code /api/exemplos/abc} num id numérico. */
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

    // ==================== Rota, verbo e formato ====================

    /**
     * Rota que não existe. Sem este handler o Spring devolve 404 com corpo VAZIO, e o app não tem o
     * que mostrar na tela.
     */
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

    // ==================== Rede de segurança ====================

    /**
     * Qualquer coisa não prevista acima. É o handler que garante o critério "nunca stack trace": o
     * detalhe técnico vai INTEIRO para o log, e o cliente recebe uma mensagem genérica.
     *
     * <p>Vazar {@code ex.getMessage()} aqui seria um vazamento de informação — mensagem de exceção
     * costuma carregar SQL, caminho de arquivo e, eventualmente, credencial.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarInesperado(Exception ex) {
        log.error("Exceção não tratada chegou ao handler global", ex);

        return resposta(CodigoErro.INTERNAL_ERROR, CodigoErro.INTERNAL_ERROR.mensagemPadrao());
    }

    // ==================== Apoio ====================

    private ResponseEntity<ErroResponse> resposta(CodigoErro codigo, String mensagem) {
        return ResponseEntity.status(codigo.status()).body(ErroResponse.de(codigo, mensagem));
    }

    private String descrever(FieldError erro) {
        return "%s: %s".formatted(erro.getField(), erro.getDefaultMessage());
    }
}
