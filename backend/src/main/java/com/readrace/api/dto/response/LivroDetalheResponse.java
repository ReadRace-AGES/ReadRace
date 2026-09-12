package com.readrace.api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LivroDetalheResponse(
        Livro livro,
        Progresso progresso,
        List<Post> posts) {

    public record Livro(
            UUID id,
            String titulo,
            String autor,
            String capaUrl,
            String genero,
            Integer totalPaginas) {}

    public record Progresso(
            Integer paginaAtual,
            Integer paginaMaximaAlcancada,
            Integer percentual,
            boolean concluido) {}

    public record Post(
            UUID id,
            Autor autor,
            String texto,
            OffsetDateTime criadoEm,
            long curtidas) {}

    public record Autor(
            String nome,
            String avatarUrl,
            Integer sequenciaDias) {}
}