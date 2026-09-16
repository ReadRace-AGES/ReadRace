package com.readrace.api.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "registro_leitura")
public class RegistroLeitura {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_biblioteca_id", nullable = false)
    private ItemBiblioteca itemBiblioteca;

    @Column(name = "ultima_pagina", nullable = false)
    private Integer ultimaPagina;

    @Column(name = "registrado_em", nullable = false)
    private OffsetDateTime registradoEm;

    public RegistroLeitura(ItemBiblioteca itemBiblioteca, int ultimaPagina) {
        this.id = UUID.randomUUID();
        this.itemBiblioteca = itemBiblioteca;
        this.ultimaPagina = ultimaPagina;
        this.registradoEm = OffsetDateTime.now();
    }
}
