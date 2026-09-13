package com.readrace.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.readrace.api.model.ItemBiblioteca;

import jakarta.persistence.LockModeType;

public interface ItemBibliotecaRepository extends JpaRepository<ItemBiblioteca, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ItemBiblioteca> findByUsuarioIdAndLivroId(UUID usuarioId, UUID livroId);
}
