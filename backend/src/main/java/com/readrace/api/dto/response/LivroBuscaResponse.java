package com.readrace.api.dto.response;

import java.util.UUID;
import java.util.stream.Collectors;

import com.readrace.api.model.Livro;

public record LivroBuscaResponse(
        UUID id,
        String titulo,
        String autor,
        String capa,
        Integer totalPaginas) {

    public static LivroBuscaResponse de(Livro livro) {
        String autores =
                livro.getLivroAutores().stream()
                        .map(vinculo -> vinculo.getAutor().getNome())
                        .collect(Collectors.joining(", "));

        return new LivroBuscaResponse(
                livro.getId(),
                livro.getTitulo(),
                livro.getLivroAutores().isEmpty() ? null : autores,
                livro.getCapaUrl(),
                livro.getTotalPaginas());
    }
}