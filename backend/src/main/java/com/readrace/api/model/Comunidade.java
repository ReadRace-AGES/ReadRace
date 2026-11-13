package com.readrace.api.model;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.readrace.api.exception.ParametroInvalidoException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "comunidade")
public class Comunidade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(name = "imagem_url", columnDefinition = "text")
    private String imagemUrl;

    @Column(name = "excluido_em")
    private OffsetDateTime excluidoEm;

    public Comunidade(String nome, String descricao, String imagemUrl) {
        this.nome = opcional(nome);
        this.descricao = opcional(descricao);
        this.imagemUrl = opcional(imagemUrl);
        if (this.nome == null
                || this.nome.length() > 120
                || this.nome.codePoints().anyMatch(Character::isISOControl)) {
            throw new ParametroInvalidoException(
                    "O nome da comunidade é obrigatório, deve ter até 120 caracteres e não pode conter quebras de linha.");
        }
        if (this.descricao != null && this.descricao.length() > 2000) {
            throw new ParametroInvalidoException("A descrição deve ter até 2.000 caracteres.");
        }
        if (this.imagemUrl != null && !imagemValida(this.imagemUrl)) {
            throw new ParametroInvalidoException(
                    "Informe uma URL HTTPS válida para a capa, sem credenciais e com até 2.048 caracteres.");
        }
    }

    private static String opcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    private static boolean imagemValida(String imagem) {
        if (imagem.length() > 2048) return false;
        try {
            URI uri = URI.create(imagem);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getRawUserInfo() == null
                    && uri.getPort() >= -1
                    && uri.getPort() <= 65535;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Comunidade outra)) {
            return false;
        }

        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
