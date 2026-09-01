package com.readrace.api.service;

import org.springframework.stereotype.Service;

import com.readrace.api.dto.response.UsuarioAtualResponse;

/**
 * Monta a resposta do usuário atual.
 *
 * <p>Existe mesmo sendo fino porque a regra do projeto é que controller não conhece domínio nem faz
 * conversão — ele recebe e devolve DTO, e a tradução acontece aqui. Quando a tabela de usuário
 * existir, é este service que passa a buscar o registro pelo id, e o controller não muda.
 */
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
