package com.readrace.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    List<Usuario>
            findByNomeContainingIgnoreCaseAndExcluidoEmIsNullOrNomeUsuarioContainingIgnoreCaseAndExcluidoEmIsNull(
                    String nome, String nomeUsuario);

    Optional<Usuario> findByIdAndExcluidoEmIsNull(UUID id);

    // Lock pessimista: serializa somas de XP concorrentes do mesmo usuário, para uma transação
    // nunca sobrescrever o xp_total que a outra acabou de gravar.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.id = :id")
    Optional<Usuario> buscarComLock(@Param("id") UUID id);
}
