package com.readrace.api.dto.response;

import java.util.UUID;
import java.util.stream.Collectors;

import com.readrace.api.model.ItemBiblioteca;
import com.readrace.api.model.Livro;

/** Identificação e progresso carregados junto da página, sem consultas por livro. */
public record LivroBibliotecaResponse(
        UUID livroId,
        String titulo,
        String autor,
        String capaUrl,
        String genero,
        int totalPaginas,
        int paginaAtual,
        int paginaMaximaAlcancada) {

    public static LivroBibliotecaResponse de(ItemBiblioteca item, String genero) {
        Livro livro = item.getLivro();
        return new LivroBibliotecaResponse(
                livro.getId(),
                livro.getTitulo(),
                autorDe(livro),
                livro.getCapaUrl(),
                genero,
                livro.getTotalPaginas(),
                item.getPaginaAtual(),
                item.getPaginaMaxima());
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
