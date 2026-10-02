package com.readrace.api.model;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;

import com.readrace.api.exception.ParametroInvalidoException;

/**
 * Posição de uma página na biblioteca: a atividade e o id do último item devolvido. É keyset, não
 * offset: um item novo no meio da lista não repete nem pula ninguém. O cliente recebe e devolve o
 * texto codificado sem interpretá-lo.
 */
public record CursorBiblioteca(Instant atividade, UUID itemId) {

    /** Antes de qualquer item: atividade no futuro distante, ordenada por id ascendente. */
    public static final CursorBiblioteca INICIO =
            new CursorBiblioteca(Instant.parse("9999-12-31T00:00:00Z"), new UUID(0L, 0L));

    private static final String SEPARADOR = "|";

    public String codificar() {
        String texto = atividade + SEPARADOR + itemId;

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    public static CursorBiblioteca decodificar(String cursor) {
        try {
            String texto =
                    new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int separador = texto.indexOf(SEPARADOR);

            if (separador < 0) {
                throw new ParametroInvalidoException("O parâmetro 'cursor' é inválido.");
            }

            return new CursorBiblioteca(
                    Instant.parse(texto.substring(0, separador)),
                    UUID.fromString(texto.substring(separador + 1)));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new ParametroInvalidoException("O parâmetro 'cursor' é inválido.");
        }
    }
}
