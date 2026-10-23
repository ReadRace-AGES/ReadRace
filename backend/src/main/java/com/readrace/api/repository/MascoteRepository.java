package com.readrace.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.readrace.api.model.Mascote;

public interface MascoteRepository extends JpaRepository<Mascote, UUID> {

    List<Mascote> findAllByOrderByOrdemAsc();

    /** O padrão de quem nunca escolheu: o primeiro da ordem, que é o do nível 1. */
    Optional<Mascote> findFirstByOrderByOrdemAsc();
}
