package com.readrace.api.repository;

import java.util.UUID;

/** Projeção de contagem de membros por grupo, para evitar uma consulta por card. */
public interface ContagemMembros {
    UUID getId();

    long getTotal();
}
