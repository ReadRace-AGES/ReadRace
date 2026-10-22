package com.readrace.api.model;

import java.util.function.Supplier;

import com.readrace.api.exception.PostInvalidoException;

/**
 * Regra de texto reutilizável pelo fluxo de publicação, sem dependência de request ou persistência.
 */
public final class ValidadorTextoPost {

    private static final int LIMITE_UNIDADES_UTF16 = 2000;

    private ValidadorTextoPost() {}

    /** Regra do post raiz: até 2.000 unidades UTF-16, {@link PostInvalidoException} se falhar. */
    public static String validar(String texto) {
        return validar(texto, LIMITE_UNIDADES_UTF16, PostInvalidoException::new);
    }

    /**
     * Retorna o texto original, sem trim ou normalização. Branco é a união de {@link
     * Character#isWhitespace(int)} e {@link Character#isSpaceChar(int)}, incluindo NBSP. O limite
     * usa {@link String#length()}, em unidades UTF-16, e inclui os espaços nas extremidades.
     */
    public static String validar(
            String texto, int limite, Supplier<? extends RuntimeException> erro) {
        if (texto == null
                || texto.length() > limite
                || texto.codePoints()
                        .allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw erro.get();
        }
        return texto;
    }
}
