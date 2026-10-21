package com.readrace.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Comunidade;

public interface ComunidadeRepository extends JpaRepository<Comunidade, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Comunidade c where c.id = :id and c.excluidoEm is null")
    Optional<Comunidade> buscarComLock(@Param("id") UUID id);

    List<Comunidade> findByNomeContainingIgnoreCaseAndExcluidoEmIsNull(String nome);
}
