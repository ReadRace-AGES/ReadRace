package com.readrace.api.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "livro")
public class Livro {

    @Id private UUID id;

    @Column(nullable = false, length = 13)
    private String isbn;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(name = "total_paginas", nullable = false)
    private Integer totalPaginas;

    @Column(name = "capa_url")
    private String capaUrl;

    protected Livro() {}

    public UUID getId() {
        return id;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitulo() {
        return titulo;
    }

    public Integer getTotalPaginas() {
        return totalPaginas;
    }

    public String getCapaUrl() {
        return capaUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Livro outro)) {
            return false;
        }

        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
