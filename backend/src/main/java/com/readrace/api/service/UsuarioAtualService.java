package com.readrace.api.service;

import org.springframework.stereotype.Service;

import com.readrace.api.dto.response.UsuarioAtualResponse;

@Service
public class UsuarioAtualService {
    private final UsuarioAtual usuarioAtual;

    public UsuarioAtualService(UsuarioAtual usuarioAtual) {
        this.usuarioAtual = usuarioAtual;
    }

    public UsuarioAtualResponse buscar() {
        return UsuarioAtualResponse.de(usuarioAtual.idDoUsuarioAtual());
    }
}
