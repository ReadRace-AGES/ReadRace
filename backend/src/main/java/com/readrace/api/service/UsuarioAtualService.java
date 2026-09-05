package com.readrace.api.service;

import org.springframework.stereotype.Service;

import com.readrace.api.dto.response.UsuarioAtualResponse;

@Service
public class UsuarioAtualService {
    private final UsuarioAtualDeSeed usuarioAtual;

    public UsuarioAtualService(UsuarioAtualDeSeed usuarioAtual) {
        this.usuarioAtual = usuarioAtual;
    }

    public UsuarioAtualResponse buscar() {
        return UsuarioAtualResponse.de(usuarioAtual.idDoUsuarioAtual());
    }
}
