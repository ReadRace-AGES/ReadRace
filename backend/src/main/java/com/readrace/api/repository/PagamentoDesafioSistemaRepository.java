package com.readrace.api.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.PagamentoDesafioSistema;

public interface PagamentoDesafioSistemaRepository
        extends JpaRepository<PagamentoDesafioSistema, UUID> {

    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO pagamento_desafio_sistema
                        (id, usuario_id, desafio, inicio_periodo, xp_pago, pago_em)
                    VALUES (:id, :usuarioId, CAST(:desafio AS desafio_sistema),
                            :inicioPeriodo, :xpPago, :pagoEm)
                    ON CONFLICT (usuario_id, desafio, inicio_periodo) DO NOTHING
                    """,
            nativeQuery = true)
    int registrarSeAusente(
            @Param("id") UUID id,
            @Param("usuarioId") UUID usuarioId,
            @Param("desafio") String desafio,
            @Param("inicioPeriodo") LocalDate inicioPeriodo,
            @Param("xpPago") Integer xpPago,
            @Param("pagoEm") OffsetDateTime pagoEm);
}
