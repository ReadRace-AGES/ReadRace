package com.readrace.api.model;

import java.util.Optional;

import com.readrace.api.exception.RecursoNaoEncontradoException;

/**
 * As quatro listas da aba Meus Livros. Três são estados de leitura; {@code favoritos} é a flag do
 * item, por isso não tem {@link StatusLeitura}.
 */
public enum ListaBiblioteca {
    FAVORITOS("favoritos", null),
    LENDO("lendo", StatusLeitura.lendo),
    DESEJO("desejo", StatusLeitura.desejo),
    LIDOS("lidos", StatusLeitura.lido);

    private final String nome;

    private final StatusLeitura statusLeitura;

    ListaBiblioteca(String nome, StatusLeitura statusLeitura) {
        this.nome = nome;
        this.statusLeitura = statusLeitura;
    }

    public String getNome() {
        return nome;
    }

    /** Vazio para {@code favoritos}, que filtra pela flag e não por estado. */
    public Optional<StatusLeitura> getStatusLeitura() {
        return Optional.ofNullable(statusLeitura);
    }

    /** O nome vem do path da URL; lista que não existe é 404, como qualquer recurso. */
    public static ListaBiblioteca de(String nome) {
        for (ListaBiblioteca lista : values()) {
            if (lista.nome.equals(nome)) {
                return lista;
            }
        }

        throw new RecursoNaoEncontradoException(
                "Lista '%s' não existe; use favoritos, lendo, desejo ou lidos.".formatted(nome));
    }
}
