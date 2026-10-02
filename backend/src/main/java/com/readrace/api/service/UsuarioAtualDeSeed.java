package com.readrace.api.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.readrace.api.model.UsuarioId;

@Component
@ConditionalOnProperty(name = "readrace.auth.modo", havingValue = "seed")
public class UsuarioAtualDeSeed implements UsuarioAtual {
    private final UsuarioId id;

    public UsuarioAtualDeSeed(@Value("${readrace.usuario-seed.id}") UUID id) {
        this.id = new UsuarioId(id);
    }

    @Override
    public UsuarioId idDoUsuarioAtual() {
        return id;
    }
}
