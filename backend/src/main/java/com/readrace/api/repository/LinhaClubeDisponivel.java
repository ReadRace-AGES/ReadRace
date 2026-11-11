package com.readrace.api.repository;

import java.util.UUID;

public interface LinhaClubeDisponivel {
    UUID getId();

    String getNome();

    String getCapaUrl();

    String getTitulo();

    String getAutor();

    long getMembros();

    String getSituacao();
}
