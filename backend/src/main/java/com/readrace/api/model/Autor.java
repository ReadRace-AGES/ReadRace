package com.readrace.api.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

@Getter
@Entity
@Table(name = "autor")
public class Autor {

    @Id private UUID id;

    @Column(nullable = false, length = 255)
    private String nome;

    protected Autor() {}
}
