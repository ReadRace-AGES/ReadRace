package com.readrace.api.service;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private static final int TAMANHO_MAX_NOME = 120;
    private static final String DOMINIO_SEM_EMAIL = "sem-email.invalid";

    private final UsuarioRepository usuarios;
    private final CognitoUserInfoClient userInfoClient;

    public ProvisionamentoUsuario(
            UsuarioRepository usuarios, CognitoUserInfoClient userInfoClient) {
        this.usuarios = usuarios;
        this.userInfoClient = userInfoClient;
    }

    /**
     * Transação própria, de escrita: o primeiro acesso costuma chegar por uma rota de leitura, cujo
     * service roda em @Transactional(readOnly = true). Participando dela, o INSERT nunca chegava ao
     * banco e a rota respondia "Usuário não encontrado" (#156). Com REQUIRES_NEW o perfil é gravado
     * antes de voltar, e a corrida tratada em UsuarioAtualDoToken lê o perfil já gravado.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Usuario provisionar(Jwt jwt) {
        CognitoUserInfo info = userInfoClient.buscar(jwt.getTokenValue());
        String sub = jwt.getSubject();
        String email = emailInformado(info);

        // Só vincula por e-mail se o provedor garantiu que o e-mail é da pessoa, e só um perfil
        // que ainda não tem vínculo: o de outra identidade nunca é tomado (#224).
        if (email != null && info.emailVerificado()) {
            var existente = usuarios.findByEmailAndExcluidoEmIsNull(email);
            if (existente.isPresent() && existente.get().getCognitoSub() == null) {
                existente.get().vincularCognito(sub);
                return existente.get();
            }
        }

        String nomeUsuario = gerarNomeUsuario(email);
        Usuario novo =
                Usuario.novoDoCognito(
                        sub,
                        nomeExibido(info, nomeUsuario),
                        nomeUsuario,
                        emailDoNovoPerfil(sub, email),
                        info.picture());
        return usuarios.save(novo);
    }

    private static String emailInformado(CognitoUserInfo info) {
        if (info.email() == null || info.email().isBlank()) {
            return null;
        }
        return info.email().trim();
    }

    /**
     * A coluna email é NOT NULL e UNIQUE. Sem e-mail, ou com um e-mail que já é de outro perfil,
     * grava um endereço sintético por identidade. O domínio .invalid é reservado (RFC 2606): nunca
     * é de uma pessoa real, e a busca por e-mail nunca casa com esse perfil.
     */
    private String emailDoNovoPerfil(String sub, String email) {
        if (email == null || usuarios.existsByEmail(email)) {
            return sub + "@" + DOMINIO_SEM_EMAIL;
        }
        return email;
    }

    /** O nome exibido nunca fica vazio nem passa da coluna; sem name, usa o nome de usuário. */
    private static String nomeExibido(CognitoUserInfo info, String nomeUsuario) {
        String nome = info.name() == null ? "" : info.name().trim();
        if (nome.isEmpty()) {
            nome = nomeUsuario;
        }
        return nome.substring(0, Math.min(nome.length(), TAMANHO_MAX_NOME));
    }

    private String gerarNomeUsuario(String email) {
        String base = prefixoDoEmail(email).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
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

    private static String prefixoDoEmail(String email) {
        if (email == null) {
            return "";
        }
        int arroba = email.indexOf('@');
        return arroba >= 0 ? email.substring(0, arroba) : email;
    }
}
