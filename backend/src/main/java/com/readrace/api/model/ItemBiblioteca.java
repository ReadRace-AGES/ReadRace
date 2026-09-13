package com.readrace.api.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
@Table(name = "item_biblioteca")
public class ItemBiblioteca {

    @Id private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "livro_id", nullable = false)
    private Livro livro;

    // Tipo enumerado nativo do Postgres: o columnDefinition faz o `validate` do Hibernate
    // aceitar a coluna, e NAMED_ENUM faz o bind como `status_leitura`, não como varchar.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status_leitura", nullable = false, columnDefinition = "status_leitura")
    private StatusLeitura statusLeitura;

    @Column(nullable = false)
    private boolean favorito;

    @Column(name = "pagina_atual", nullable = false)
    private Integer paginaAtual;

    @Column(name = "pagina_maxima", nullable = false)
    private Integer paginaMaxima;

    @Column(name = "adicionado_em", nullable = false)
    private OffsetDateTime adicionadoEm;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ItemBiblioteca outro)) {
            return false;
        }

        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
