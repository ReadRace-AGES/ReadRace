package com.readrace.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * O que o cliente PODE mandar. Repare que não tem id — quem gera é o banco.
 *
 * <p>Nunca receba a entidade direto no controller: isso deixaria o cliente escrever em qualquer
 * campo da tabela.
 */
public record ExemploRequest(
        @NotBlank(message = "informe o nome")
                @Size(max = 100, message = "nome deve ter no máximo 100 caracteres")
                String nome,
        @Size(max = 255, message = "descrição deve ter no máximo 255 caracteres")
                String descricao) {}
