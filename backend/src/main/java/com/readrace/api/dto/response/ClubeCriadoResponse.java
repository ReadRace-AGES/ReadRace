package com.readrace.api.dto.response;

import java.util.UUID;

import com.readrace.api.model.ClubeDoLivro;

public record ClubeCriadoResponse(UUID id, String nome, Livro livro) {
    public static ClubeCriadoResponse de(ClubeDoLivro clube) {
        return new ClubeCriadoResponse(
                clube.getId(),
                clube.getNome(),
                new Livro(clube.getLivro().getTitulo(), clube.getLivro().getCapaUrl()));
    }

    public record Livro(String titulo, String capaUrl) {}
}
