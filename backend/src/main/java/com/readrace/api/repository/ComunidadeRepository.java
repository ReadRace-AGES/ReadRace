package com.readrace.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.readrace.api.model.Comunidade;

public interface ComunidadeRepository extends JpaRepository<Comunidade, UUID> {

    List<Comunidade> findByNomeContainingIgnoreCaseAndExcluidoEmIsNull(String nome);
}
