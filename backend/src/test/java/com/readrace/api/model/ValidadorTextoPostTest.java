package com.readrace.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.readrace.api.exception.PostInvalidoException;

class ValidadorTextoPostTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n", "\r\n", "\u00A0", " \t\r\n\u00A0 "})
    void rejeita_texto_ausente_ou_em_branco(String texto) {
        assertThatThrownBy(() -> ValidadorTextoPost.validar(texto))
                .isInstanceOf(PostInvalidoException.class);
    }

    @Test
    void rejeita_os_demais_caracteres_da_regra_de_branco() {
        String brancos =
                "\u000B\f\u001C\u001D\u001E\u001F\u1680\u2000\u2001\u2002\u2003"
                        + "\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u2028\u2029\u202F\u205F\u3000";

        brancos.codePoints()
                .forEach(
                        c ->
                                assertThatThrownBy(
                                                () ->
                                                        ValidadorTextoPost.validar(
                                                                Character.toString(c)))
                                        .isInstanceOf(PostInvalidoException.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2000})
    void aceita_texto_nos_limites(int tamanho) {
        String texto = "a".repeat(tamanho);

        assertThat(ValidadorTextoPost.validar(texto)).isSameAs(texto);
    }

    @Test
    void rejeita_texto_com_2001_unidades_utf16() {
        assertThatThrownBy(() -> ValidadorTextoPost.validar("a".repeat(2001)))
                .isInstanceOf(PostInvalidoException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 999})
    void aceita_emoji_contando_duas_unidades_utf16(int paresAdicionais) {
        String texto = "😀".repeat(1 + paresAdicionais);
        assertThat(texto.length()).isEqualTo(2 * (1 + paresAdicionais));

        assertThat(ValidadorTextoPost.validar(texto)).isSameAs(texto);
    }

    @Test
    void aceita_emoji_ao_final_de_2000_unidades_utf16() {
        String texto = "a".repeat(1998) + "😀";
        assertThat(texto.length()).isEqualTo(2000);

        assertThat(ValidadorTextoPost.validar(texto)).isSameAs(texto);
    }

    @Test
    void rejeita_emoji_que_ultrapassa_o_limite_utf16() {
        String texto = "a".repeat(1999) + "😀";
        assertThat(texto.length()).isEqualTo(2001);

        assertThatThrownBy(() -> ValidadorTextoPost.validar(texto))
                .isInstanceOf(PostInvalidoException.class);
    }

    @Test
    void preserva_espacos_nas_extremidades_e_texto_sem_normalizar() {
        String texto = " \t\u00A0 cafe\u0301 😀 \r\n ";

        assertThat(ValidadorTextoPost.validar(texto)).isSameAs(texto);
    }

    @Test
    void conta_espacos_nas_extremidades_no_limite() {
        String texto = " " + "a".repeat(1999) + " ";

        assertThatThrownBy(() -> ValidadorTextoPost.validar(texto))
                .isInstanceOf(PostInvalidoException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\u0085", "\u200B", "\uFEFF"})
    void aceita_caracteres_fora_da_regra_de_branco(String texto) {
        assertThat(ValidadorTextoPost.validar(texto)).isSameAs(texto);
    }

    @Test
    void aplica_limite_e_erro_informados() {
        IllegalStateException erro = new IllegalStateException();

        assertThat(ValidadorTextoPost.validar("abc", 3, () -> erro)).isEqualTo("abc");
        assertThatThrownBy(() -> ValidadorTextoPost.validar("abcd", 3, () -> erro)).isSameAs(erro);
        assertThatThrownBy(() -> ValidadorTextoPost.validar(" ", 3, () -> erro)).isSameAs(erro);
    }
}
