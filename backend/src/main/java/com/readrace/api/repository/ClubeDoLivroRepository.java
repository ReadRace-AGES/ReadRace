package com.readrace.api.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.ClubeDoLivro;

public interface ClubeDoLivroRepository extends JpaRepository<ClubeDoLivro, UUID> {

    Optional<ClubeDoLivro> findByIdAndExcluidoEmIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ClubeDoLivro c where c.id = :id and c.excluidoEm is null")
    Optional<ClubeDoLivro> buscarComLock(@Param("id") UUID id);
}
