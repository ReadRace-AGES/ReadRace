package com.readrace.api.dto.response;

import java.util.UUID;

/**
 * {@code status} é o {@code StatusLeitura} do item após a gravação (#155). {@code progresso} só vem
 * preenchido quando o livro entra em "lendo", com a recompensa do registro de leitura.
 */
public record AdicionarLivroResponse(
        UUID livroId,
        String titulo,
        String capaUrl,
        String status,
        boolean favorito,
        ProgressoLeituraResponse progresso) {

    public AdicionarLivroResponse(
            UUID livroId, String titulo, String capaUrl, String status, boolean favorito) {
        this(livroId, titulo, capaUrl, status, favorito, null);
    }
}
