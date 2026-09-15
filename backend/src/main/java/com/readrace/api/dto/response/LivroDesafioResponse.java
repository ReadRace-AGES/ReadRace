package com.readrace.api.dto.response;

import java.util.UUID;
import java.util.stream.Collectors;

import com.readrace.api.model.Livro;

public record LivroDesafioResponse(UUID id, String titulo, String autor, String capaUrl) {

    public static LivroDesafioResponse de(Livro livro) {
        String autores =
                livro.getLivroAutores().stream()
                        .map(vinculo -> vinculo.getAutor().getNome())
                        .collect(Collectors.joining(", "));

        return new LivroDesafioResponse(
                livro.getId(),
                livro.getTitulo(),
                livro.getLivroAutores().isEmpty() ? null : autores,
                livro.getCapaUrl());
    }
}
