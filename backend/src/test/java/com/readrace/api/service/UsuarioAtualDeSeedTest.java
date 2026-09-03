package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.readrace.api.model.UsuarioId;

@DisplayName("UsuarioAtualDeSeed")
class UsuarioAtualDeSeedTest {
    private static final UUID ID_DE_SEED = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void deve_devolver_sempre_o_id_configurado() {
        UsuarioAtualProvider provider = new UsuarioAtualDeSeed(ID_DE_SEED);

        assertThat(provider.idDoUsuarioAtual()).isEqualTo(new UsuarioId(ID_DE_SEED));
    }

    @Test
    void deve_devolver_o_mesmo_id_em_chamadas_repetidas() {
        UsuarioAtualProvider provider = new UsuarioAtualDeSeed(ID_DE_SEED);

        assertThat(provider.idDoUsuarioAtual()).isEqualTo(provider.idDoUsuarioAtual());
    }
}
