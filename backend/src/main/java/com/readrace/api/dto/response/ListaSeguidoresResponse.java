package com.readrace.api.dto.response;

import java.util.List;
import java.util.UUID;

import com.readrace.api.model.Usuario;

public record ListaSeguidoresResponse(List<Leitor> usuarios) {
    public static ListaSeguidoresResponse de(List<Usuario> usuarios) {
        return new ListaSeguidoresResponse(usuarios.stream().map(Leitor::de).toList());
    }

    public record Leitor(UUID id, String nome, String nomeUsuario, String avatarUrl) {
        public static Leitor de(Usuario usuario) {
            return new Leitor(
                    usuario.getId(),
                    usuario.getNome(),
                    usuario.getNomeUsuario(),
                    usuario.getAvatarUrl());
        }
    }
}
