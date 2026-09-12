package com.readrace.api.dto.response;

import java.util.UUID;

import com.readrace.api.model.Usuario;

public record UsuarioBuscaResponse(
        UUID id, String nome, String username, String avatar, String titulo) {

    public static UsuarioBuscaResponse de(Usuario usuario) {
        return new UsuarioBuscaResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getNomeUsuario(),
                usuario.getAvatarUrl(),
                usuario.getTitulo());
    }
}
