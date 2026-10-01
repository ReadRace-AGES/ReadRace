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

    Optional<Usuario> findByCognitoSubAndExcluidoEmIsNull(String cognitoSub);

    Optional<Usuario> findByEmailAndExcluidoEmIsNull(String email);

    boolean existsByNomeUsuario(String nomeUsuario);

    // O registro de leitura escreve nesta linha. O lock serializa registros simultâneos do mesmo
    // usuário, para um não sobrescrever o que o outro gravou.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT usuario
            FROM Usuario usuario
            WHERE usuario.id = :id
              AND usuario.excluidoEm IS NULL
            """)
    Optional<Usuario> buscarAtivoComLock(@Param("id") UUID id);
}
