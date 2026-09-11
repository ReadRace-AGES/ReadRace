package com.readrace.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.readrace.api.model.ItemBiblioteca;

public interface ItemBibliotecaRepository extends JpaRepository<ItemBiblioteca, UUID> {

    Optional<ItemBiblioteca> findByUsuarioIdAndLivroId(UUID usuarioId, UUID livroId);
}