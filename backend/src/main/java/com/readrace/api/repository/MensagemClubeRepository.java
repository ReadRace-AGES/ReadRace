package com.readrace.api.repository;

import com.readrace.api.model.MensagemClube;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface MensagemClubeRepository
        extends JpaRepository<MensagemClube, UUID> {

    List<MensagemClube> findByClube_IdOrderByEnviadaEmDesc(
            UUID clubeId,
            Pageable pageable);

    List<MensagemClube> findByClube_IdAndEnviadaEmGreaterThanOrderByEnviadaEmAsc(
            UUID clubeId,
            OffsetDateTime depois);
}