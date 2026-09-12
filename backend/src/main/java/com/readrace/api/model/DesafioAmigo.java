package com.readrace.api.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "desafio_amigo")
public class DesafioAmigo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criador_id", nullable = false)
    private Usuario criador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oponente_id", nullable = false)
    private Usuario oponente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livro_id")
    private Livro livro;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(columnDefinition = "text")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_meta", nullable = false, columnDefinition = "tipo_meta_desafio")
    private TipoMetaDesafio tipoMeta;

    @Column(name = "meta_valor", nullable = false)
    private Integer metaValor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "status_desafio")
    private StatusDesafio status;

    @Column(name = "inicio_em", nullable = false)
    private OffsetDateTime inicioEm;

    @Column(name = "fim_em", nullable = false)
    private OffsetDateTime fimEm;

    public DesafioAmigo(
            Usuario criador,
            Usuario oponente,
            Livro livro,
            String titulo,
            String descricao,
            TipoMetaDesafio tipoMeta,
            Integer metaValor,
            OffsetDateTime inicioEm,
            OffsetDateTime fimEm) {
        this.criador = criador;
        this.oponente = oponente;
        this.livro = livro;
        this.titulo = titulo;
        this.descricao = descricao;
        this.tipoMeta = tipoMeta;
        this.metaValor = metaValor;
        this.status = StatusDesafio.PENDENTE;
        this.inicioEm = inicioEm;
        this.fimEm = fimEm;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof DesafioAmigo outro)) {
            return false;
        }

        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
