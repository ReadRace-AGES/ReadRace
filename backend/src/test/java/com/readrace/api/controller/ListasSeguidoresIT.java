package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.service.PerfilService;
import com.readrace.api.service.SeguirService;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ListasSeguidoresIT {
    private static final UUID EU = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OUTRO = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID TERCEIRO = UUID.fromString("00000000-0000-0000-0000-000000000003");
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvcTester mvc;
    @Autowired private SeguirService seguir;
    @Autowired private PerfilService perfis;

    @BeforeEach
    void limparRelacoes() {
        jdbc.update("DELETE FROM seguir");
    }

    private void relacao(UUID seguidor, UUID seguido, String data) {
        jdbc.update(
                "INSERT INTO seguir (id, seguidor_id, seguido_id, criado_em) VALUES (?, ?, ?, CAST(? AS timestamptz))",
                UUID.randomUUID(),
                seguidor,
                seguido,
                data);
    }

    private String url(UUID id, String lista) {
        return "/api/usuarios/" + id + "/" + lista;
    }

    @Test
    void listaSeguidoresComContratoOrdemETotalDoPerfil() {
        relacao(OUTRO, EU, "2020-01-01T00:00:00Z");
        relacao(TERCEIRO, EU, "2021-01-01T00:00:00Z");
        var antes = jdbc.queryForMap("SELECT * FROM usuario WHERE id = ?", EU);
        assertThat(mvc.get().uri(url(EU, "seguidores")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.usuarios[*].id")
                .asList()
                .containsExactly(TERCEIRO.toString(), OUTRO.toString());
        assertThat(mvc.get().uri(url(EU, "seguidores")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.usuarios[0]")
                .asMap()
                .containsOnlyKeys("id", "nome", "nomeUsuario", "avatarUrl")
                .containsEntry(
                        "nomeUsuario",
                        jdbc.queryForObject(
                                "SELECT nome_usuario FROM usuario WHERE id = ?",
                                String.class,
                                TERCEIRO));
        assertThat(seguir.seguidores(EU).usuarios()).hasSize((int) perfis.buscar(EU).seguidores());
        assertThat(jdbc.queryForMap("SELECT * FROM usuario WHERE id = ?", EU)).isEqualTo(antes);
    }

    @Test
    void listaSeguindoDeOutroLeitorPodeIncluirUsuarioAtual() {
        relacao(OUTRO, TERCEIRO, "2020-01-01T00:00:00Z");
        relacao(OUTRO, EU, "2021-01-01T00:00:00Z");
        assertThat(mvc.get().uri(url(OUTRO, "seguindo")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.usuarios[*].id")
                .asList()
                .containsExactly(EU.toString(), TERCEIRO.toString());
        assertThat(seguir.seguindo(OUTRO).usuarios())
                .hasSize((int) perfis.buscar(OUTRO).seguindo());
    }

    @Test
    void novoSeguimentoAparecePrimeiroESairRemoveDaLista() {
        relacao(EU, OUTRO, "2020-01-01T00:00:00Z");
        assertThat(mvc.post().uri(url(TERCEIRO, "seguidores/eu"))).hasStatusOk();
        assertThat(mvc.get().uri(url(EU, "seguindo")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.usuarios[*].id")
                .asList()
                .containsExactly(TERCEIRO.toString(), OUTRO.toString());
        assertThat(mvc.delete().uri(url(TERCEIRO, "seguidores/eu"))).hasStatusOk();
        assertThat(seguir.seguindo(EU).usuarios()).extracting(u -> u.id()).containsExactly(OUTRO);
    }

    @Test
    void listasVaziasRetornamArrayVazio() {
        for (String lista : new String[] {"seguidores", "seguindo"}) {
            assertThat(mvc.get().uri(url(EU, lista)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.usuarios")
                    .asList()
                    .isEmpty();
        }
    }

    @Test
    void perfilInexistenteOuExcluidoRetorna404() {
        jdbc.update("UPDATE usuario SET excluido_em = now() WHERE id = ?", OUTRO);
        for (UUID id : new UUID[] {UUID.randomUUID(), OUTRO}) {
            for (String lista : new String[] {"seguidores", "seguindo"}) {
                assertThat(mvc.get().uri(url(id, lista)))
                        .hasStatus(HttpStatus.NOT_FOUND)
                        .bodyJson()
                        .extractingPath("$.code")
                        .isEqualTo("USUARIO_NAO_ENCONTRADO");
            }
        }
    }

    @Test
    void usuariosExcluidosNaoEntramNasListasNemNasContagens() {
        relacao(OUTRO, EU, "2020-01-01T00:00:00Z");
        relacao(EU, OUTRO, "2020-01-01T00:00:00Z");
        relacao(TERCEIRO, EU, "2020-01-01T00:00:00Z");
        relacao(EU, TERCEIRO, "2020-01-01T00:00:00Z");
        jdbc.update("UPDATE usuario SET excluido_em = now() WHERE id = ?", OUTRO);
        assertThat(seguir.seguidores(EU).usuarios())
                .extracting(u -> u.id())
                .containsExactly(TERCEIRO);
        assertThat(seguir.seguindo(EU).usuarios())
                .extracting(u -> u.id())
                .containsExactly(TERCEIRO);
        var perfil = perfis.buscar(EU);
        assertThat(perfil.seguidores()).isEqualTo(1);
        assertThat(perfil.seguindo()).isEqualTo(1);
    }
}
