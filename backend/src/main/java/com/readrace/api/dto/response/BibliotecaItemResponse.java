package com.readrace.api.dto.response;

import java.util.UUID;

public record BibliotecaItemResponse(UUID livroId, String statusLeitura, boolean favorito) {}
