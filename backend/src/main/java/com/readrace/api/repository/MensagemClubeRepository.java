package com.readrace.api.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.readrace.api.model.MensagemClube;

public interface MensagemClubeRepository extends JpaRepository<MensagemClube, UUID> {

    List<MensagemClube> findByClube_IdOrderByEnviadaEmDesc(UUID clubeId, Pageable pageable);

    List<MensagemClube> findByClube_IdAndEnviadaEmGreaterThanOrderByEnviadaEmAsc(
            UUID clubeId, OffsetDateTime depois);
}
