package com.readrace.api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.MascoteResponse;
import com.readrace.api.dto.response.MascotesResponse;
import com.readrace.api.exception.MascoteNaoEncontradoException;
import com.readrace.api.exception.UsuarioNaoEncontradoException;
import com.readrace.api.model.Mascote;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.MascoteRepository;
import com.readrace.api.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class MascoteService {
    private final MascoteRepository mascotes;
    private final UsuarioRepository usuarios;
    private final UsuarioAtual usuarioAtual;

    public MascoteService(
            MascoteRepository mascotes, UsuarioRepository usuarios, UsuarioAtual usuarioAtual) {
        this.mascotes = mascotes;
        this.usuarios = usuarios;
        this.usuarioAtual = usuarioAtual;
    }

    public MascotesResponse listarDoUsuarioAtual() {
        Usuario usuario = usuarioDoRequest();
        List<Mascote> todos = mascotes.findAllByOrderByOrdemAsc();
        // Quem nunca escolheu fica com o primeiro da ordem, o do nível 1.
        UUID escolhido =
                usuario.getMascoteId() != null ? usuario.getMascoteId() : todos.get(0).getId();
        return new MascotesResponse(
                todos.stream()
                        .map(m -> MascotesResponse.Item.de(m, usuario.getNivel(), escolhido))
                        .toList());
    }

    @Transactional
    public MascoteResponse escolherDoUsuarioAtual(UUID mascoteId) {
        Mascote mascote =
                mascotes.findById(mascoteId).orElseThrow(MascoteNaoEncontradoException::new);
        usuarioDoRequest().escolherMascote(mascote);
        return MascoteResponse.de(mascote);
    }

    /** O mascote que aparece no perfil: o escolhido ou, se nunca escolheu, o do nível 1. */
    public Mascote escolhidoDe(Usuario usuario) {
        var escolhido =
                usuario.getMascoteId() == null
                        ? mascotes.findFirstByOrderByOrdemAsc()
                        : mascotes.findById(usuario.getMascoteId());
        // A V15 cadastra os mascotes e a FK garante que o escolhido existe.
        return escolhido.orElseThrow();
    }

    private Usuario usuarioDoRequest() {
        return usuarios.findByIdAndExcluidoEmIsNull(usuarioAtual.idDoUsuarioAtual().valor())
                .orElseThrow(UsuarioNaoEncontradoException::new);
    }
}
