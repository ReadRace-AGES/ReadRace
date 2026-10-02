package com.readrace.api.service;

import com.readrace.api.model.UsuarioId;

/** Quem está fazendo o request. Implementado pelo seed (dev/testes) ou pelo token do Cognito. */
public interface UsuarioAtual {
    UsuarioId idDoUsuarioAtual();
}
