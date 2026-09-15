package com.readrace.api.dto.response;

import java.util.UUID;
import java.util.stream.Collectors;

import com.readrace.api.model.Livro;

/** Um livro como a aba Meus Livros o exibe: só o que cabe numa capa com título e autor. */
public record LivroBibliotecaResponse(UUID livroId, String titulo, String autor, String capaUrl) {

    public static LivroBibliotecaResponse de(Livro livro) {
        return new LivroBibliotecaResponse(
                livro.getId(), livro.getTitulo(), autorDe(livro), livro.getCapaUrl());
    }

    /** Mesma regra da busca (#72): autores na ordem do vínculo, separados por vírgula. */
    static String autorDe(Livro livro) {
        if (livro.getLivroAutores().isEmpty()) {
            return null;
        }

        return livro.getLivroAutores().stream()
                .map(vinculo -> vinculo.getAutor().getNome())
                .collect(Collectors.joining(", "));
    }
}
