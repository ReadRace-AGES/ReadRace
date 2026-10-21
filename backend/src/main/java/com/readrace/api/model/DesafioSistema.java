package com.readrace.api.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/** Definições e calendário também utilizados pela medição de períodos passados. */
public enum DesafioSistema {
    MENSAL("Desafio da leitura mensal", "Leia 2 livros este mês", 2, "livros", 200),
    SEMANAL(null, "Leia 70 páginas essa semana", 70, "paginas", 100);

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final String titulo;
    private final String descricao;
    private final int meta;
    private final String unidade;
    private final int xp;

    DesafioSistema(String titulo, String descricao, int meta, String unidade, int xp) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.meta = meta;
        this.unidade = unidade;
        this.xp = xp;
    }

    public String titulo() {
        return titulo;
    }

    public String descricao() {
        return descricao;
    }

    public int meta() {
        return meta;
    }

    public String unidade() {
        return unidade;
    }

    public int xp() {
        return xp;
    }

    public Periodo periodo(LocalDate data) {
        LocalDate inicio =
                this == MENSAL
                        ? data.withDayOfMonth(1)
                        : data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new Periodo(inicio, this == MENSAL ? inicio.plusMonths(1) : inicio.plusWeeks(1));
    }

    public record Periodo(LocalDate inicio, LocalDate fimExclusivo) {
        public Instant inicioEm() {
            return inicio.atStartOfDay(FUSO).toInstant();
        }

        public Instant fimEm() {
            return fimExclusivo.atStartOfDay(FUSO).toInstant();
        }

        public int diasRestantes(LocalDate hoje) {
            return Math.toIntExact(ChronoUnit.DAYS.between(hoje, fimExclusivo));
        }
    }
}
