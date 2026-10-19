package com.readrace.api.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.DesafioSistemaResponse;
import com.readrace.api.dto.response.DesafiosSistemaResponse;
import com.readrace.api.model.DesafioSistema;
import com.readrace.api.repository.RegistroLeituraRepository;

@Service
@Transactional(readOnly = true)
public class DesafiosSistemaService {
    private final RegistroLeituraRepository registros;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;

    public DesafiosSistemaService(
            RegistroLeituraRepository registros, UsuarioAtual usuarioAtual, Clock clock) {
        this.registros = registros;
        this.usuarioAtual = usuarioAtual;
        this.clock = clock;
    }

    public DesafiosSistemaResponse consultar() {
        Instant agora = clock.instant();
        LocalDate hoje = agora.atZone(DesafioSistema.FUSO).toLocalDate();
        Medicao medicao = medir(usuarioAtual.idDoUsuarioAtual().valor(), hoje, agora);
        return new DesafiosSistemaResponse(
                DesafioSistemaResponse.de(
                        DesafioSistema.MENSAL,
                        medicao.mensal(),
                        DesafioSistema.MENSAL.periodo(hoje).diasRestantes(hoje)),
                DesafioSistemaResponse.de(
                        DesafioSistema.SEMANAL,
                        medicao.semanal(),
                        DesafioSistema.SEMANAL.periodo(hoje).diasRestantes(hoje)));
    }

    /**
     * Mede os períodos que contêm a data informada, sem limitar os resultados às metas. O instante
     * de consulta impede que registros futuros influenciem inclusive períodos passados.
     */
    public Medicao medir(UUID usuarioId, LocalDate dataDoPeriodo, Instant consultadoEm) {
        var mes = DesafioSistema.MENSAL.periodo(dataDoPeriodo);
        var semana = DesafioSistema.SEMANAL.periodo(dataDoPeriodo);
        var medicao =
                registros.medirSistema(
                        usuarioId,
                        mes.inicioEm(),
                        mes.fimEm(),
                        semana.inicioEm(),
                        semana.fimEm(),
                        consultadoEm);
        return new Medicao(medicao.getMensal(), medicao.getSemanal());
    }

    public record Medicao(long mensal, long semanal) {}
}
