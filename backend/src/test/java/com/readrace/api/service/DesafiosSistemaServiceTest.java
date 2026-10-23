package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.readrace.api.model.DesafioSistema;
import com.readrace.api.repository.RegistroLeituraRepository;

class DesafiosSistemaServiceTest {
    @Test
    void consulta_captura_um_instante_e_usa_data_local_em_vez_da_data_utc() {
        var registros = mock(RegistroLeituraRepository.class);
        var clock = mock(Clock.class);
        Instant agora = Instant.parse("2026-11-01T01:00:00Z");
        when(clock.instant()).thenReturn(agora);
        UUID usuario = UUID.randomUUID();
        var medicao = mock(RegistroLeituraRepository.MedicaoSistema.class);
        when(medicao.getMensal()).thenReturn(3L);
        when(medicao.getSemanal()).thenReturn(125L);
        when(registros.medirSistema(
                        usuario,
                        Instant.parse("2026-10-01T03:00:00Z"),
                        Instant.parse("2026-11-01T03:00:00Z"),
                        Instant.parse("2026-10-26T03:00:00Z"),
                        Instant.parse("2026-11-02T03:00:00Z"),
                        agora))
                .thenReturn(medicao);
        var service = new DesafiosSistemaService(registros, new UsuarioAtualDeSeed(usuario), clock);
        var resposta = service.consultar();
        assertThat(resposta.mensal().progresso()).isEqualTo(2);
        assertThat(resposta.mensal().cumprido()).isTrue();
        assertThat(resposta.mensal().diasRestantes()).isEqualTo(1);
        assertThat(resposta.semanal().progresso()).isEqualTo(70);
        assertThat(resposta.semanal().cumprido()).isTrue();
        assertThat(resposta.semanal().diasRestantes()).isEqualTo(2);
        verify(clock, times(1)).instant();
        verifyNoMoreInteractions(clock);
    }

    @Test
    void medicao_de_periodo_informado_preserva_resultado_bruto() {
        var registros = mock(RegistroLeituraRepository.class);
        var medicao = mock(RegistroLeituraRepository.MedicaoSistema.class);
        UUID usuario = UUID.randomUUID();
        LocalDate data = LocalDate.of(2024, 2, 29);
        Instant agora = Instant.parse("2026-10-18T15:00:00Z");
        var mes = DesafioSistema.MENSAL.periodo(data);
        var semana = DesafioSistema.SEMANAL.periodo(data);
        when(registros.medirSistema(
                        usuario,
                        mes.inicioEm(),
                        mes.fimEm(),
                        semana.inicioEm(),
                        semana.fimEm(),
                        agora))
                .thenReturn(medicao);
        when(medicao.getMensal()).thenReturn(4L);
        when(medicao.getSemanal()).thenReturn(170L);
        var service =
                new DesafiosSistemaService(
                        registros,
                        new UsuarioAtualDeSeed(usuario),
                        Clock.fixed(agora, ZoneOffset.UTC));
        assertThat(service.medir(usuario, data, agora))
                .isEqualTo(new DesafiosSistemaService.Medicao(4, 170));
    }
}
