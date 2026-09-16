package com.readrace.api.dto.response;

import java.util.List;

public record BuscaResponse<T>(String tipo, List<T> itens) {}
