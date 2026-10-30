package com.readrace.api.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.model.DesafioSistema;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.PagamentoDesafioSistemaRepository;
import com.readrace.api.service.DesafiosSistemaService.Medicao;

@Service
public class PagamentoDesafioSistemaService {
    private final PagamentoDesafioSistemaRepository pagamentos;

    public PagamentoDesafioSistemaService(PagamentoDesafioSistemaRepository pagamentos) {
        this.pagamentos = pagamentos;
    }

    // O chamador mantém o usuário gerenciado e bloqueado durante as duas medições e o crédito.
    @Transactional(propagation = Propagation.MANDATORY)
    public void pagarCruzamentos(
            Usuario usuario,
            LocalDate dataDoPeriodo,
            OffsetDateTime pagoEm,
            Medicao anterior,
            Medicao posterior) {
        for (DesafioSistema desafio : DesafioSistema.values()) {
            long antes = progresso(anterior, desafio);
            long depois = progresso(posterior, desafio);
            if (antes < desafio.meta()
                    && depois >= desafio.meta()
                    && pagamentos.registrarSeAusente(
                                    UUID.randomUUID(),
                                    usuario.getId(),
                                    desafio.name(),
                                    desafio.periodo(dataDoPeriodo).inicio(),
                                    desafio.xp(),
                                    pagoEm)
                            == 1) {
                usuario.receberXp(desafio.xp());
            }
        }
    }

    private long progresso(Medicao medicao, DesafioSistema desafio) {
        return switch (desafio) {
            case MENSAL -> medicao.mensal();
            case SEMANAL -> medicao.semanal();
        };
    }
}
