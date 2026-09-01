package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.readrace.api.model.UsuarioId;

/**
 * Teste unitário puro: sem contexto Spring, sem banco, sem Testcontainers.
 *
 * <p>É rápido porque o provider é uma classe boba de propósito. Toda a complexidade de "quem é o
 * usuário" vai morar na implementação que lê o token, e ela substitui esta — não convive com ela.
 */
@DisplayName("UsuarioAtualDeSeed")
class UsuarioAtualDeSeedTest {

    private static final UUID ID_DE_SEED = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void deve_devolver_sempre_o_id_configurado() {
        UsuarioAtualProvider provider = new UsuarioAtualDeSeed(ID_DE_SEED);

        assertThat(provider.idDoUsuarioAtual()).isEqualTo(new UsuarioId(ID_DE_SEED));
    }

    /**
     * "Consistently returns the fixed seed ID", nas palavras do critério de validação #2 da issue
     * #10: duas chamadas não podem divergir.
     */
    @Test
    void deve_devolver_o_mesmo_id_em_chamadas_repetidas() {
        UsuarioAtualProvider provider = new UsuarioAtualDeSeed(ID_DE_SEED);

        assertThat(provider.idDoUsuarioAtual()).isEqualTo(provider.idDoUsuarioAtual());
    }
}
