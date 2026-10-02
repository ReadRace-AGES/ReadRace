package com.readrace.api.service;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.adapter.cognito.CognitoUserInfo;
import com.readrace.api.adapter.cognito.CognitoUserInfoClient;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.UsuarioRepository;

/** Cria (ou vincula) o Usuario do ReadRace no primeiro request de uma identidade do Cognito. */
@Service
@ConditionalOnProperty(name = "readrace.auth.modo", havingValue = "cognito")
public class ProvisionamentoUsuario {
    private static final int TAMANHO_MAX_NOME_USUARIO = 50;

    private final UsuarioRepository usuarios;
    private final CognitoUserInfoClient userInfoClient;

    public ProvisionamentoUsuario(
            UsuarioRepository usuarios, CognitoUserInfoClient userInfoClient) {
        this.usuarios = usuarios;
        this.userInfoClient = userInfoClient;
    }

    @Transactional
    public Usuario provisionar(Jwt jwt) {
        CognitoUserInfo info = userInfoClient.buscar(jwt.getTokenValue());

        // Só vincula por e-mail se o provedor garantiu que o e-mail é da pessoa.
        if (info.emailVerificado()) {
            var existente = usuarios.findByEmailAndExcluidoEmIsNull(info.email());
            if (existente.isPresent()) {
                existente.get().vincularCognito(jwt.getSubject());
                return existente.get();
            }
        }

        String nome = info.name() != null ? info.name() : info.email();
        Usuario novo =
                Usuario.novoDoCognito(
                        jwt.getSubject(),
                        nome,
                        gerarNomeUsuario(info.email()),
                        info.email(),
                        info.picture());
        return usuarios.save(novo);
    }

    private String gerarNomeUsuario(String email) {
        String base =
                email.substring(0, email.indexOf('@'))
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9_]", "");
        if (base.isBlank()) {
            base = "leitor";
        }
        base = base.substring(0, Math.min(base.length(), TAMANHO_MAX_NOME_USUARIO - 5));

        String candidato = base;
        while (usuarios.existsByNomeUsuario(candidato)) {
            candidato = base + ThreadLocalRandom.current().nextInt(1000, 10000);
        }
        return candidato;
    }
}
