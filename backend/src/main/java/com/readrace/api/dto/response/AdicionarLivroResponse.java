package com.readrace.api.dto.response;

import java.util.UUID;

/** {@code status} é o {@code StatusLeitura} do item após a gravação (#155). */
public record AdicionarLivroResponse(
        UUID livroId, String titulo, String capaUrl, String status, boolean favorito) {}
