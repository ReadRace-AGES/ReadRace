package com.readrace.api.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.PerfilResponse;
import com.readrace.api.exception.UsuarioNaoEncontradoException;
import com.readrace.api.model.CurvaDeNivel;
import com.readrace.api.repository.PerfilRepository;
import com.readrace.api.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class PerfilService {
    private final UsuarioRepository usuarios;
    private final PerfilRepository perfis;
    private final UsuarioAtualDeSeed usuarioAtual;

    public PerfilService(
            UsuarioRepository usuarios, PerfilRepository perfis, UsuarioAtualDeSeed usuarioAtual) {
        this.usuarios = usuarios;
        this.perfis = perfis;
        this.usuarioAtual = usuarioAtual;
    }

    public PerfilResponse buscarDoUsuarioAtual() {
        return buscar(usuarioAtual.idDoUsuarioAtual().valor());
    }

    public PerfilResponse buscar(UUID id) {
        var usuario =
                usuarios.findByIdAndExcluidoEmIsNull(id)
                        .orElseThrow(UsuarioNaoEncontradoException::new);
        var resumo = perfis.resumo(id);
        var conquistas = perfis.conquistas(id);
        int nivel = usuario.getNivel();
        int xpTotal = usuario.getXpTotal();
        return new PerfilResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getNomeUsuario(),
                usuario.getAvatarUrl(),
                usuario.getTitulo(),
                nivel,
                xpTotal,
                CurvaDeNivel.xpNoNivel(xpTotal, nivel),
                CurvaDeNivel.xpDoNivel(nivel),
                resumo.seguidores(),
                resumo.seguindo(),
                new PerfilResponse.Estatisticas(
                        resumo.livrosLidos(),
                        resumo.paginasLidas(),
                        usuario.getDiasConsecutivos(),
                        conquistas.stream().filter(PerfilResponse.Conquista::desbloqueada).count()),
                conquistas,
                perfis.favoritos(id));
    }
}
