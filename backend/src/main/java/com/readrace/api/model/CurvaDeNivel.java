package com.readrace.api.model;

public final class CurvaDeNivel {
    private static final double FATOR = 45.0;

    private CurvaDeNivel() {}

    public static int xpDoNivel(int nivel) {
        return (int) Math.floor(FATOR * nivel * Math.sqrt(nivel));
    }

    public static int xpParaChegarNoNivel(int nivel) {
        int total = 0;
        for (int n = 1; n < nivel; n++) {
            total += xpDoNivel(n);
        }
        return total;
    }

    public static int xpNoNivel(int xpTotal, int nivel) {
        return xpTotal - xpParaChegarNoNivel(nivel);
    }
}
