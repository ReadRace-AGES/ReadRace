package com.readrace.api.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import com.readrace.api.model.Usuario;
import com.readrace.api.model.UsuarioId;
import com.readrace.api.repository.UsuarioRepository;

/**
 * Resolve o usuário pelo "sub" do JWT do Cognito. Request-scoped: os services chamam
 * idDoUsuarioAtual() várias vezes por request e o banco só é consultado na primeira.
 */
@Component
@RequestScope
@ConditionalOnProperty(name = "readrace.auth.modo", havingValue = "cognito")
public class UsuarioAtualDoToken implements UsuarioAtual {
    private final UsuarioRepository usuarios;
    private final ProvisionamentoUsuario provisionamento;
    private UsuarioId cache;

    public UsuarioAtualDoToken(UsuarioRepository usuarios, ProvisionamentoUsuario provisionamento) {
        this.usuarios = usuarios;
        this.provisionamento = provisionamento;
    }

    @Override
    public UsuarioId idDoUsuarioAtual() {
        if (cache == null) {
            cache = new UsuarioId(resolver().getId());
        }
        return cache;
    }

    private Usuario resolver() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String sub = jwt.getSubject();

        return usuarios.findByCognitoSubAndExcluidoEmIsNull(sub)
                .orElseGet(
                        () -> {
                            try {
                                return provisionamento.provisionar(jwt);
                            } catch (DataIntegrityViolationException corrida) {
                                // Dois requests simultâneos no primeiro login: o outro já criou.
                                return usuarios.findByCognitoSubAndExcluidoEmIsNull(sub)
                                        .orElseThrow(() -> corrida);
                            }
                        });
    }
}
