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
import com.readrace.api.model.CurvaDeNivel;
import com.readrace.api.service.PerfilService;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PerfilControllerIT {
    private static final UUID FIXO = UUID.fromString("00000000-0000-0000-0000-000000000001");
    @Autowired private MockMvcTester mvc;
    @Autowired private PerfilService service;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void retornaPerfilDoSeedSemRecalcularXpNivelOuPaginas() {
        var perfil = service.buscar(FIXO);
        assertThat(perfil.nivel()).isEqualTo(5);
        assertThat(perfil.xpAtual()).isEqualTo(2450);
        assertThat(perfil.titulo()).isEqualTo("Leitor iniciante");
        assertThat(perfil.estatisticas().livrosLidos()).isEqualTo(5);
        assertThat(perfil.estatisticas().paginasLidas())
                .isEqualTo(
                        jdbc.queryForObject(
                                "SELECT sum(pagina_maxima) FROM item_biblioteca WHERE usuario_id = ?",
                                Long.class,
                                FIXO));
        assertThat(perfil.estatisticas().conquistas()).isEqualTo(5);
        assertThat(perfil.conquistas()).hasSize(6);
        assertThat(perfil.conquistas().stream().filter(c -> !c.desbloqueada()).toList())
                .singleElement()
                .satisfies(c -> assertThat(c.data()).isNull());
        assertThat(perfil.livrosFavoritos()).hasSize(3);
        assertThat(perfil.seguidores())
                .isEqualTo(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM seguir WHERE seguido_id = ?",
                                Long.class,
                                FIXO));
        assertThat(perfil.seguindo())
                .isEqualTo(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM seguir WHERE seguidor_id = ?",
                                Long.class,
                                FIXO));
        assertThat(mvc.get().uri("/api/usuarios/" + FIXO + "/perfil"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.xpAtual")
                .isEqualTo(2450);
    }

    @Test
    void retornaOutroUsuarioSemBibliotecaComTodasConquistasBloqueadas() {
        var perfil = service.buscar(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(perfil.nome()).isEqualTo("Ana Silva");
        assertThat(perfil.xpAtual()).isEqualTo(1850);
        assertThat(perfil.estatisticas().livrosLidos()).isZero();
        assertThat(perfil.estatisticas().paginasLidas()).isZero();
        assertThat(perfil.estatisticas().conquistas()).isZero();
        assertThat(perfil.conquistas())
                .hasSize(6)
                .allSatisfy(
                        c -> {
                            assertThat(c.desbloqueada()).isFalse();
                            assertThat(c.data()).isNull();
                        });
        assertThat(perfil.livrosFavoritos()).isEmpty();
    }

    @Test
    void usuarioInexistenteRetornaEnvelope404() {
        assertThat(mvc.get().uri("/api/usuarios/" + UUID.randomUUID() + "/perfil"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("USUARIO_NAO_ENCONTRADO");
    }

    @Test
    @Transactional
    void usuarioExcluidoNaoEhExposto() {
        jdbc.update("UPDATE usuario SET excluido_em = now() WHERE id = ?", FIXO);
        assertThat(mvc.get().uri("/api/usuarios/" + FIXO + "/perfil"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void meuPerfilResolveOUsuarioAtualSemReceberId() {
        assertThat(mvc.get().uri("/api/me/perfil"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(FIXO.toString());

        var perfil = service.buscarDoUsuarioAtual();
        assertThat(perfil.xpDoNivel()).isEqualTo(CurvaDeNivel.xpDoNivel(perfil.nivel()));
        assertThat(perfil.xpNoNivel())
                .isEqualTo(CurvaDeNivel.xpNoNivel(perfil.xpAtual(), perfil.nivel()));
    }
}
