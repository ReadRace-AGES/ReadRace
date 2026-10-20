package com.readrace.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "livro_autor")
public class LivroAutor {

    @EmbeddedId private LivroAutorId id;

    @MapsId("livroId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "livro_id", nullable = false)
    private Livro livro;

    @MapsId("autorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Autor autor;

    @Column(nullable = false)
    private Short ordem;

    /** Vínculo criado junto com um {@link Livro} novo, a partir do catálogo externo (#155). */
    public LivroAutor(Livro livro, Autor autor, short ordem) {
        this.id = new LivroAutorId(livro.getId(), autor.getId());
        this.livro = livro;
        this.autor = autor;
        this.ordem = ordem;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof LivroAutor outro)) {
            return false;
        }

        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
