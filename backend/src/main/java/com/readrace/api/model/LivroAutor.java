package com.readrace.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "livro_autor")
public class LivroAutor {

    @EmbeddedId
    private LivroAutorId id;

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

    protected LivroAutor() {}

    public LivroAutorId getId() {
        return id;
    }

    public Livro getLivro() {
        return livro;
    }

    public Autor getAutor() {
        return autor;
    }

    public Short getOrdem() {
        return ordem;
    }
}