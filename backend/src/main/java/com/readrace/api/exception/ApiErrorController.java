package com.readrace.api.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.ErroResponse;

/**
 * Rede de segurança para o que NÃO passa pelo {@link GlobalExceptionHandler}.
 *
 * <p>O {@code @RestControllerAdvice} só enxerga exceção que atravessa o DispatcherServlet. Erro
 * estourado antes disso — num filtro de servlet, ou pelo próprio Tomcat — é despachado para {@code
 * /error}, e ali o Spring Boot responderia com o formato do {@code BasicErrorController}:
 *
 * <pre>{@code
 * {"timestamp":"...","status":999,"error":"None"}
 * }</pre>
 *
 * <p>Que é um terceiro formato, diferente do contrato da issue #10. Declarar um bean de {@link
 * ErrorController} substitui o padrão do Boot, porque o auto-configure dele é
 * {@code @ConditionalOnMissingBean}.
 *
 * <p>Fica no pacote {@code exception} e não em {@code controller} de propósito: não é endpoint de
 * API, é o alvo de despacho de erro do container. Toda a tradução de exceção para HTTP mora junta.
 *
 * <p>Hoje esta classe quase não é exercitada — o advice cobre tudo. Ela existe para quando o Spring
 * Security entrar: falha de autenticação acontece dentro de um filtro, longe do advice.
 */
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<ErroResponse> tratar(HttpServletRequest request) {
        CodigoErro codigo = codigoPara(statusDe(request));

        return ResponseEntity.status(codigo.status()).body(ErroResponse.de(codigo));
    }

    /** O container guarda o status original neste atributo antes de redespachar para /error. */
    private HttpStatus statusDe(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        HttpStatus resolvido = HttpStatus.resolve(Integer.parseInt(status.toString()));

        return resolvido != null ? resolvido : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private CodigoErro codigoPara(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> CodigoErro.ROUTE_NOT_FOUND;
            case METHOD_NOT_ALLOWED -> CodigoErro.METHOD_NOT_ALLOWED;
            case UNSUPPORTED_MEDIA_TYPE -> CodigoErro.UNSUPPORTED_MEDIA_TYPE;
            case BAD_REQUEST -> CodigoErro.MALFORMED_REQUEST;
            default -> CodigoErro.INTERNAL_ERROR;
        };
    }
}
