package com.readrace.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.readrace.api.exception.ComentarioInvalidoException;
import com.readrace.api.exception.PostInvalidoException;

class PostTest {

    private static final UUID AUTOR_ID = UUID.randomUUID();
    private static final UUID CLUBE_ID = UUID.randomUUID();
    private static final UUID POST_PAI_ID = UUID.randomUUID();

    @Test
    void cria_post_raiz_de_clube_com_id_e_data_do_servidor() {
        OffsetDateTime antes = OffsetDateTime.now();

        Post post = Post.criarRaizDeClube(AUTOR_ID, CLUBE_ID, "Minha leitura de hoje.");

        OffsetDateTime depois = OffsetDateTime.now();
        assertThat(post.getAutorId()).isEqualTo(AUTOR_ID);
        assertThat(post.getClubeId()).isEqualTo(CLUBE_ID);
        assertThat(post.getConteudo()).isEqualTo("Minha leitura de hoje.");
        assertThat(post.getId()).isNotNull();
        assertThat(post.getId().version()).isEqualTo(4);
        assertThat(post.getCriadoEm()).isBetween(antes, depois);
        assertThat(post.getLivroId()).isNull();
        assertThat(post.getPostPaiId()).isNull();
        assertThat(post.getExcluidoEm()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", "\r\n", "\u00A0", " \t\r\n\u00A0 "})
    void rejeita_texto_ausente_ou_em_branco(String texto) {
        assertThatThrownBy(() -> Post.criarRaizDeClube(AUTOR_ID, CLUBE_ID, texto))
                .isInstanceOf(PostInvalidoException.class);
    }

    @Test
    void rejeita_texto_acima_do_limite_utf16() {
        String texto = "a".repeat(1999) + "😀";

        assertThatThrownBy(() -> Post.criarRaizDeClube(AUTOR_ID, CLUBE_ID, texto))
                .isInstanceOf(PostInvalidoException.class);
    }

    @Test
    void preserva_texto_original_com_espacos_e_quebras_de_linha() {
        String texto = " \t\u00A0 Minha leitura\r\ncontinua aqui.\n ";

        Post post = Post.criarRaizDeClube(AUTOR_ID, CLUBE_ID, texto);

        assertThat(post.getConteudo()).isSameAs(texto);
    }

    @Test
    void cria_comentario_com_pai_e_sem_escopo_proprio() {
        OffsetDateTime antes = OffsetDateTime.now();

        Post comentario = Post.criarComentario(AUTOR_ID, POST_PAI_ID, "Concordo!");

        OffsetDateTime depois = OffsetDateTime.now();
        assertThat(comentario.getAutorId()).isEqualTo(AUTOR_ID);
        assertThat(comentario.getPostPaiId()).isEqualTo(POST_PAI_ID);
        assertThat(comentario.getConteudo()).isEqualTo("Concordo!");
        assertThat(comentario.getId()).isNotNull();
        assertThat(comentario.getCriadoEm()).isBetween(antes, depois);
        assertThat(comentario.getClubeId()).isNull();
        assertThat(comentario.getComunidadeId()).isNull();
        assertThat(comentario.getLivroId()).isNull();
        assertThat(comentario.getExcluidoEm()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", "\u00A0"})
    void rejeita_comentario_ausente_ou_em_branco(String texto) {
        assertThatThrownBy(() -> Post.criarComentario(AUTOR_ID, POST_PAI_ID, texto))
                .isInstanceOf(ComentarioInvalidoException.class);
    }

    @Test
    void comentario_aceita_1000_e_rejeita_1001_unidades_utf16() {
        assertThat(Post.criarComentario(AUTOR_ID, POST_PAI_ID, "a".repeat(1000)).getConteudo())
                .hasSize(1000);
        assertThatThrownBy(() -> Post.criarComentario(AUTOR_ID, POST_PAI_ID, "a".repeat(1001)))
                .isInstanceOf(ComentarioInvalidoException.class);
    }
}
