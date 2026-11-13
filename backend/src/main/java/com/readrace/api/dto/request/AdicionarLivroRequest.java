package com.readrace.api.dto.request;

/**
 * {@code lista} é "lido", "desejo", "favorito" (#155) ou "lendo". Em "lendo", {@code pagina} é
 * obrigatória e segue as mesmas regras do registro de leitura.
 */
public record AdicionarLivroRequest(String volumeId, String lista, Object pagina) {

    public AdicionarLivroRequest(String volumeId, String lista) {
        this(volumeId, lista, null);
    }
}
