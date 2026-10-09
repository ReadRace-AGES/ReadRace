package com.readrace.api.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "nome_usuario", nullable = false, length = 50)
    private String nomeUsuario;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Column(nullable = false)
    private Integer nivel;

    @Column(name = "xp_total", nullable = false)
    private Integer xpTotal;

    @Column(name = "dias_consecutivos", nullable = false)
    private Integer diasConsecutivos;

    @Column(name = "excluido_em")
    private OffsetDateTime excluidoEm;

    @Column(name = "ultima_leitura_em")
    private LocalDate ultimaLeituraEm;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "cognito_sub", length = 255)
    private String cognitoSub;

    public void receberXp(int xp) {
        xpTotal += xp;
        nivel = CurvaDeNivel.nivelDoXp(xpTotal);
    }

    public void registrarLeitura(LocalDate hoje) {
        diasConsecutivos = SequenciaDeLeitura.aposLeitura(diasConsecutivos, ultimaLeituraEm, hoje);
        ultimaLeituraEm = hoje;
    }

    public int sequenciaExibida(LocalDate hoje) {
        return SequenciaDeLeitura.exibida(diasConsecutivos, ultimaLeituraEm, hoje);
    }

    /** Cria o usuário no primeiro login pelo Cognito. */
    public static Usuario novoDoCognito(
            String cognitoSub, String nome, String nomeUsuario, String email, String avatarUrl) {
        Usuario u = new Usuario();
        u.id = UUID.randomUUID();
        u.cognitoSub = cognitoSub;
        u.nome = nome;
        u.nomeUsuario = nomeUsuario;
        u.email = email;
        u.avatarUrl = avatarUrl;
        // Os defaults do banco não valem: o Hibernate envia estes campos explicitamente.
        u.nivel = 1;
        u.xpTotal = 0;
        u.diasConsecutivos = 0;
        return u;
    }

    /** Liga um usuário já existente (ex.: seed) à identidade do Cognito. */
    public void vincularCognito(String cognitoSub) {
        this.cognitoSub = cognitoSub;
    }

    /** Edição pelas Configurações de perfil. null = manter o valor atual. */
    public void atualizarPerfil(String nome, String nomeUsuario) {
        if (nome != null) {
            this.nome = nome;
        }
        if (nomeUsuario != null) {
            this.nomeUsuario = nomeUsuario;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Usuario outro)) {
            return false;
        }

        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getTitulo() {
        if (nivel <= 10) {
            return "Leitor iniciante";
        } else if (nivel <= 20) {
            return "Leitor explorador";
        } else if (nivel <= 30) {
            return "Leitor dedicado";
        } else if (nivel <= 40) {
            return "Leitor experiente";
        } else {
            return "Mestre da leitura";
        }
    }
}
