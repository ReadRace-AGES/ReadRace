package com.readrace.api.model;

import java.util.UUID;

/**
 * O identificador de um usuário.
 *
 * <p>É um <b>value object</b>: um tipo definido só pelo valor que carrega, imutável, sem identidade
 * própria. Não é entidade JPA — mora em {@code model} porque é conceito de domínio, e domínio é o
 * que este pacote guarda.
 *
 * <p><b>Por que existir em vez de passar UUID cru.</b> {@code UUID} significa "um UUID qualquer": o
 * compilador não distingue id de usuário de id de livro. Isto compila e está errado:
 *
 * <pre>{@code
 * void adicionar(UUID usuario, UUID livro) { ... }
 * adicionar(idDoLivro, idDoUsuario);   // troca silenciosa
 * }</pre>
 *
 * <p>Com o tipo envolvendo o valor, o erro deixa de compilar. O custo é escrever {@code .valor()}
 * nas bordas — onde o id vira DTO, coluna de banco ou parâmetro de query.
 *
 * @param valor o UUID em si; nunca nulo
 */
public record UsuarioId(UUID valor) {

    public UsuarioId {
        if (valor == null) {
            throw new IllegalArgumentException("id de usuário não pode ser nulo");
        }
    }
}
