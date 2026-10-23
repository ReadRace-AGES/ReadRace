package com.readrace.api.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.ListaSeguidoresResponse;
import com.readrace.api.dto.response.SeguirResponse;
import com.readrace.api.exception.NaoPodeSeguirASiException;
import com.readrace.api.exception.UsuarioNaoEncontradoException;
import com.readrace.api.repository.SeguirRepository;
import com.readrace.api.repository.UsuarioRepository;

@Service
public class SeguirService {
    private final SeguirRepository seguir;
    private final UsuarioRepository usuarios;
    private final UsuarioAtual usuarioAtual;

    public SeguirService(
            SeguirRepository seguir, UsuarioRepository usuarios, UsuarioAtual usuarioAtual) {
        this.seguir = seguir;
        this.usuarios = usuarios;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional
    public SeguirResponse seguir(UUID seguidoId) {
        UUID eu = validar(seguidoId);
        seguir.criarSeAusente(UUID.randomUUID(), eu, seguidoId);
        return resposta(eu, seguidoId);
    }

    @Transactional(readOnly = true)
    public ListaSeguidoresResponse seguidores(UUID usuarioId) {
        usuarios.findByIdAndExcluidoEmIsNull(usuarioId)
                .orElseThrow(UsuarioNaoEncontradoException::new);
        return ListaSeguidoresResponse.de(seguir.buscarSeguidores(usuarioId));
    }

    @Transactional(readOnly = true)
    public ListaSeguidoresResponse seguindo(UUID usuarioId) {
        usuarios.findByIdAndExcluidoEmIsNull(usuarioId)
                .orElseThrow(UsuarioNaoEncontradoException::new);
        return ListaSeguidoresResponse.de(seguir.buscarSeguindo(usuarioId));
    }

    @Transactional
    public SeguirResponse deixarDeSeguir(UUID seguidoId) {
        UUID eu = validar(seguidoId);
        seguir.removerSeExistente(eu, seguidoId);
        return resposta(eu, seguidoId);
    }

    private UUID validar(UUID seguidoId) {
        UUID eu = usuarioAtual.idDoUsuarioAtual().valor();
        if (eu.equals(seguidoId)) {
            throw new NaoPodeSeguirASiException();
        }
        usuarios.findByIdAndExcluidoEmIsNull(seguidoId)
                .orElseThrow(UsuarioNaoEncontradoException::new);
        return eu;
    }

    private SeguirResponse resposta(UUID eu, UUID seguidoId) {
        return new SeguirResponse(
                seguir.countBySeguidoId(seguidoId),
                seguir.existsBySeguidorIdAndSeguidoId(eu, seguidoId),
                seguir.existsBySeguidorIdAndSeguidoId(seguidoId, eu));
    }
}
