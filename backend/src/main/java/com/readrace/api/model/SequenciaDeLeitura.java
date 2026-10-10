package com.readrace.api.model;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

// Sequência de dias com leitura (decisão do gestor em 2026-09-18): pausas de até 7 dias não a
// quebram; 8 dias ou mais desde a última leitura a encerram.
public final class SequenciaDeLeitura {

    // O dia é o de São Paulo para todo mundo, até existir fuso por usuário.
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private static final int DIAS_PARA_PERDER = 8;

    private SequenciaDeLeitura() {}

    public static LocalDate hoje() {
        return LocalDate.now(FUSO);
    }

    public static int aposLeitura(int sequencia, LocalDate ultimaLeitura, LocalDate hoje) {
        if (ultimaLeitura == null) {
            return 1;
        }

        long dias = diasDesde(ultimaLeitura, hoje);

        if (dias == 0) {
            return sequencia;
        }

        return dias >= DIAS_PARA_PERDER ? 1 : sequencia + 1;
    }

    // A coluna só é escrita ao registrar leitura: quem exibe é que zera a sequência encerrada.
    public static int exibida(int sequencia, LocalDate ultimaLeitura, LocalDate hoje) {
        if (ultimaLeitura == null || diasDesde(ultimaLeitura, hoje) >= DIAS_PARA_PERDER) {
            return 0;
        }

        return sequencia;
    }

    // A chama do app acende quando a leitura de hoje já foi registrada.
    public static boolean leuHoje(LocalDate ultimaLeitura, LocalDate hoje) {
        return ultimaLeitura != null && diasDesde(ultimaLeitura, hoje) == 0;
    }

    // Quantos dias faltam para a sequência acabar sem leitura: 7 se leu ontem, 1 se acaba amanhã.
    // Nulo quando não há o que avisar: nunca leu, já leu hoje ou a sequência já acabou.
    public static Integer diasAtePerder(LocalDate ultimaLeitura, LocalDate hoje) {
        if (ultimaLeitura == null) {
            return null;
        }

        long dias = diasDesde(ultimaLeitura, hoje);

        if (dias == 0 || dias >= DIAS_PARA_PERDER) {
            return null;
        }

        return (int) (DIAS_PARA_PERDER - dias);
    }

    // A sequência nunca conta o futuro: uma data depois de hoje vale como hoje.
    private static long diasDesde(LocalDate ultimaLeitura, LocalDate hoje) {
        return Math.max(0, ChronoUnit.DAYS.between(ultimaLeitura, hoje));
    }
}
