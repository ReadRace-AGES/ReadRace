package com.readrace.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.readrace.api.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    List<Usuario>
            findByNomeContainingIgnoreCaseAndExcluidoEmIsNullOrNomeUsuarioContainingIgnoreCaseAndExcluidoEmIsNull(
                    String nome, String nomeUsuario);

    Optional<Usuario> findByIdAndExcluidoEmIsNull(UUID id);
}
