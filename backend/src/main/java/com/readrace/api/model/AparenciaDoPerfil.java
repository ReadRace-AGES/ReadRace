package com.readrace.api.model;

import java.util.Set;

/**
 * Chaves que o app sabe desenhar: os bonecos do avatar e os planos de fundo do perfil. A arte fica
 * no app; aqui só se valida a chave antes de gravar.
 */
public final class AparenciaDoPerfil {

    public static final Set<String> AVATARES =
            Set.of(
                    "boneco-1",
                    "boneco-2",
                    "boneco-3",
                    "boneco-4",
                    "boneco-5",
                    "boneco-6",
                    "boneco-7",
                    "boneco-8",
                    "boneco-9",
                    "boneco-10",
                    "boneco-11",
                    "boneco-12");

    public static final Set<String> PLANOS_DE_FUNDO =
            Set.of("cenario", "ceu", "por-do-sol", "noite", "floresta", "oceano");

    private AparenciaDoPerfil() {}
}
