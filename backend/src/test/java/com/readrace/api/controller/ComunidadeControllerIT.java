package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ComunidadeControllerIT {
    private static final UUID EU = UUID.fromString("00000000-0000-0000-0000-000000000001");
    @Autowired private MockMvcTester mvc;
    @Autowired private JdbcTemplate jdbc;

    private UUID criarComunidade() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO comunidade (id, nome) VALUES (?, 'Teste de participação')", id);
        return id;
    }

    private String membros(UUID id) {
        return "/api/comunidades/" + id + "/membros/eu";
    }

    @Test
    void unicoAdministradorRecebeMensagemEPermaneceMembro() {
        UUID id = criarComunidade();
        jdbc.update(
                "INSERT INTO membro_comunidade (id, comunidade_id, usuario_id, cargo_comunidade) VALUES (?, ?, ?, 'administrador')",
                UUID.randomUUID(),
                id,
                EU);
        assertThat(mvc.delete().uri(membros(id)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("code", "UNICO_ADMINISTRADOR")
                .containsEntry(
                        "message",
                        "Você é o único administrador desta comunidade e não pode sair.");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM membro_comunidade WHERE comunidade_id = ?",
                                Integer.class,
                                id))
                .isEqualTo(1);
    }

    @Test
    void comunidadeInexistenteRetornaMensagemEmTodasAsRotas() {
        UUID id = UUID.randomUUID();
        assertThat(mvc.get().uri("/api/comunidades/" + id))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("Comunidade não encontrada.");
        assertThat(mvc.post().uri(membros(id)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("Comunidade não encontrada.");
        assertThat(mvc.delete().uri(membros(id)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.message")
                .isEqualTo("Comunidade não encontrada.");
    }

    @Test
    void seguirESairSaoIdempotentesSemAlterarUsuarioNemApagarPosts() {
        UUID id = criarComunidade();
        var usuarioAntes = jdbc.queryForMap("SELECT * FROM usuario WHERE id = ?", EU);
        for (int i = 0; i < 2; i++) {
            assertThat(mvc.post().uri(membros(id)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$")
                    .asMap()
                    .containsEntry("sigo", true)
                    .containsEntry("seguidores", 1);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT cargo_comunidade::text FROM membro_comunidade WHERE comunidade_id = ?",
                                String.class,
                                id))
                .isEqualTo("membro");
        UUID post = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO post (id, autor_id, comunidade_id, conteudo) VALUES (?, ?, ?, 'Post preservado')",
                post,
                EU,
                id);
        for (int i = 0; i < 2; i++) {
            assertThat(mvc.delete().uri(membros(id)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$")
                    .asMap()
                    .containsEntry("sigo", false)
                    .containsEntry("seguidores", 0);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM post WHERE id = ?", Integer.class, post))
                .isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT * FROM usuario WHERE id = ?", EU))
                .isEqualTo(usuarioAntes);
    }
}
