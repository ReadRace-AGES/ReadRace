package com.readrace.api.model;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

@Entity
@Table(name = "livro")
public class Livro {

    @Id
    private UUID id;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(name = "total_paginas", nullable = false)
    private Integer totalPaginas;

    @Column(name = "capa_url", columnDefinition = "text")
    private String capaUrl;

    @OneToMany(mappedBy = "livro")
    @OrderBy("ordem ASC")
    private List<LivroAutor> livroAutores = new ArrayList<>();

    protected Livro() {}

    public UUID getId() {
        return id;
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

    public List<LivroAutor> getLivroAutores() {
        return livroAutores;
    }
}
