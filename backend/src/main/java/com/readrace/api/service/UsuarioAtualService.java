package com.readrace.api.service;

import org.springframework.stereotype.Service;

import com.readrace.api.dto.response.UsuarioAtualResponse;

@Service
public class UsuarioAtualService {
    private final UsuarioAtualProvider usuarioAtualProvider;

    public UsuarioAtualService(UsuarioAtualProvider usuarioAtualProvider) {
        this.usuarioAtualProvider = usuarioAtualProvider;
    }

    public UsuarioAtualResponse buscar() {
        return UsuarioAtualResponse.de(usuarioAtualProvider.idDoUsuarioAtual());
    }
}
