package com.readrace.api.model;

import com.readrace.api.exception.PostInvalidoException;

/**
 * Regra de texto reutilizável pelo fluxo de publicação, sem dependência de request ou persistência.
 */
public final class ValidadorTextoPost {

    private static final int LIMITE_UNIDADES_UTF16 = 2000;

    private ValidadorTextoPost() {}

    /**
     * Retorna o texto original, sem trim ou normalização. Branco é a união de {@link
     * Character#isWhitespace(int)} e {@link Character#isSpaceChar(int)}, incluindo NBSP. O limite
     * usa {@link String#length()}, em unidades UTF-16, e inclui os espaços nas extremidades.
     */
    public static String validar(String texto) {
        if (texto == null
                || texto.length() > LIMITE_UNIDADES_UTF16
                || texto.codePoints()
                        .allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new PostInvalidoException();
        }
        return texto;
    }
}
