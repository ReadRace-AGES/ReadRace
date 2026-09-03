package com.readrace.api.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.ErroResponse;

@RestController
public class ApiErrorController implements ErrorController {
    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<ErroResponse> tratar(HttpServletRequest request) {
        CodigoErro codigo = codigoPara(statusDe(request));

        return ResponseEntity.status(codigo.status()).body(ErroResponse.de(codigo));
    }

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
