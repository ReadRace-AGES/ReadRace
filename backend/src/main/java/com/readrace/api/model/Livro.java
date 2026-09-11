package com.readrace.api.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "livro")
public class Livro {

    @Id private UUID id;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(name = "total_paginas", nullable = false)
    private Integer totalPaginas;

    @Column(name = "capa_url", columnDefinition = "text")
    private String capaUrl;

    @OneToMany(mappedBy = "livro")
    @OrderBy("ordem ASC")
    private List<LivroAutor> livroAutores = new ArrayList<>();
}
