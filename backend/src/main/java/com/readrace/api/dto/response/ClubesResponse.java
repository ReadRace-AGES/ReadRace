package com.readrace.api.dto.response;

import java.util.List;
import java.util.UUID;

import com.readrace.api.repository.LinhaClubeDisponivel;

public record ClubesResponse(List<Clube> clubes) {
    public record Clube(
            UUID id, String nome, String capaUrl, Livro livro, long membros, String situacao) {
        public static Clube de(LinhaClubeDisponivel linha) {
            return new Clube(
                    linha.getId(),
                    linha.getNome(),
                    linha.getCapaUrl(),
                    new Livro(linha.getTitulo(), linha.getAutor()),
                    linha.getMembros(),
                    linha.getSituacao());
        }
    }

    public record Livro(String titulo, String autor) {}
}
