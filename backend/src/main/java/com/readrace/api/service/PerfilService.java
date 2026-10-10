package com.readrace.api.service;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.request.AtualizarPerfilRequest;
import com.readrace.api.dto.response.PerfilResponse;
import com.readrace.api.exception.PerfilInvalidoException;
import com.readrace.api.exception.UsernameEmUsoException;
import com.readrace.api.exception.UsuarioNaoEncontradoException;
import com.readrace.api.model.CurvaDeNivel;
import com.readrace.api.model.SequenciaDeLeitura;
import com.readrace.api.repository.PerfilRepository;
import com.readrace.api.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class PerfilService {
    private static final int TAMANHO_MAX_NOME = 120;
    private static final Pattern FORMATO_USERNAME = Pattern.compile("^[a-z0-9_]{3,50}$");
    private final UsuarioRepository usuarios;
    private final PerfilRepository perfis;
    private final UsuarioAtual usuarioAtual;

    public PerfilService(
            UsuarioRepository usuarios, PerfilRepository perfis, UsuarioAtual usuarioAtual) {
        this.usuarios = usuarios;
        this.perfis = perfis;
        this.usuarioAtual = usuarioAtual;
    }

    public PerfilResponse buscarDoUsuarioAtual() {
        return buscar(usuarioAtual.idDoUsuarioAtual().valor());
    }

    @Transactional
    public PerfilResponse atualizarDoUsuarioAtual(AtualizarPerfilRequest request) {
        UUID id = usuarioAtual.idDoUsuarioAtual().valor();
        var usuario =
                usuarios.findByIdAndExcluidoEmIsNull(id)
                        .orElseThrow(UsuarioNaoEncontradoException::new);

        String nome = request.nome() == null ? null : request.nome().trim();
        if (nome != null && (nome.isEmpty() || nome.length() > TAMANHO_MAX_NOME)) {
            throw new PerfilInvalidoException(
                    "O nome é obrigatório e deve ter até 120 caracteres.");
        }

        String username = request.username() == null ? null : request.username().trim();
        if (username != null) {
            if (!FORMATO_USERNAME.matcher(username).matches()) {
                throw new PerfilInvalidoException(
                        "Use de 3 a 50 caracteres: letras minúsculas sem acento, números ou"
                                + " sublinhado.");
            }
            boolean mudou = !username.equals(usuario.getNomeUsuario());
            if (mudou && usuarios.existsByNomeUsuario(username)) {
                throw new UsernameEmUsoException();
            }
        }

        usuario.atualizarPerfil(nome, username);
        try {
            usuarios.flush();
        } catch (DataIntegrityViolationException e) {
            throw new UsernameEmUsoException();
        }

        return buscar(id);
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
                        usuario.sequenciaExibida(SequenciaDeLeitura.hoje()),
                        conquistas.stream().filter(PerfilResponse.Conquista::desbloqueada).count()),
                conquistas,
                perfis.favoritos(id));
    }
}
