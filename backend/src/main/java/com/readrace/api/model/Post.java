package com.readrace.api.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.readrace.api.exception.ComentarioInvalidoException;

@Entity
@Table(name = "post")
public class Post {

    private static final int LIMITE_COMENTARIO = 1000;

    @Id private UUID id;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(name = "livro_id")
    private UUID livroId;

    /**
     * Escopo do post quando ele é de um clube. Um post é de clube OU de comunidade, nunca dos dois.
     */
    @Column(name = "clube_id")
    private UUID clubeId;

    @Column(name = "comunidade_id")
    private UUID comunidadeId;

    @Column(name = "post_pai_id")
    private UUID postPaiId;

    @Column(nullable = false)
    private String conteudo;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "excluido_em")
    private OffsetDateTime excluidoEm;

    protected Post() {}

    /** Cria um post raiz de clube, sem vínculo com livro ou comunidade. */
    public static Post criarRaizDeClube(UUID autorId, UUID clubeId, String texto) {
        String conteudo = ValidadorTextoPost.validar(texto);
        Post post = new Post();
        post.id = UUID.randomUUID();
        post.autorId = autorId;
        post.clubeId = clubeId;
        post.conteudo = conteudo;
        post.criadoEm = OffsetDateTime.now();
        return post;
    }

    /**
     * Cria um comentário: post com pai e sem clube, livro ou comunidade, porque o escopo vem do pai
     * ({@code chk_comentario_sem_escopo}). Limite de 1.000 unidades UTF-16 (#150).
     */
    public static Post criarComentario(UUID autorId, UUID postPaiId, String texto) {
        String conteudo =
                ValidadorTextoPost.validar(
                        texto, LIMITE_COMENTARIO, ComentarioInvalidoException::new);
        Post post = new Post();
        post.id = UUID.randomUUID();
        post.autorId = autorId;
        post.postPaiId = postPaiId;
        post.conteudo = conteudo;
        post.criadoEm = OffsetDateTime.now();
        return post;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAutorId() {
        return autorId;
    }

    public UUID getLivroId() {
        return livroId;
    }

    public UUID getClubeId() {
        return clubeId;
    }

    public UUID getComunidadeId() {
        return comunidadeId;
    }

    public UUID getPostPaiId() {
        return postPaiId;
    }

    public String getConteudo() {
        return conteudo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getExcluidoEm() {
        return excluidoEm;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Post outro)) {
            return false;
        }

        return id != null && id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
