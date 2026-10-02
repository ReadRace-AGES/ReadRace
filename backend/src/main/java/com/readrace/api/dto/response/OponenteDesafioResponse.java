package com.readrace.api.dto.response;

import java.util.UUID;

import com.readrace.api.model.Usuario;

public record OponenteDesafioResponse(UUID id, String username, String avatarUrl) {

    public static OponenteDesafioResponse de(Usuario usuario) {
        return new OponenteDesafioResponse(
                usuario.getId(), usuario.getNomeUsuario(), usuario.getAvatarUrl());
    }
}
