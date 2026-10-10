package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
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
        assertThat(perfil.nivel()).isEqualTo(7);
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
        assertThat(perfil.nivel()).isEqualTo(7);
        assertThat(perfil.xpAtual()).isEqualTo(2450);
        assertThat(perfil.xpNoNivel()).isEqualTo(521);
        assertThat(perfil.xpDoNivel()).isEqualTo(833);
    }

    private MvcTestResult patchPerfil(String json) {
        return mvc.patch()
                .uri("/api/me/perfil")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private String usernameNoBanco() {
        return jdbc.queryForObject(
                "SELECT nome_usuario FROM usuario WHERE id = ?", String.class, FIXO);
    }

    @Test
    @Transactional
    void atualizaNomeEUsernameDoUsuarioAtual() {
        var resposta = patchPerfil("{\"nome\": \"Daniel R.\", \"username\": \"daniel_r\"}");

        assertThat(resposta).hasStatusOk();
        assertThat(resposta).bodyJson().extractingPath("$.nome").isEqualTo("Daniel R.");
        assertThat(resposta).bodyJson().extractingPath("$.username").isEqualTo("daniel_r");
        assertThat(usernameNoBanco()).isEqualTo("daniel_r");
    }

    @Test
    @Transactional
    void mandarSoONomeMantemOUsername() {
        var resposta = patchPerfil("{\"nome\": \"Daniel R.\"}");

        assertThat(resposta).hasStatusOk();
        assertThat(resposta).bodyJson().extractingPath("$.username").isEqualTo("danielribeiro");
    }

    @Test
    @Transactional
    void usernameDeOutroUsuarioRetorna409ENaoGrava() {
        var resposta = patchPerfil("{\"username\": \"anasilva\"}");

        assertThat(resposta).hasStatus(HttpStatus.CONFLICT);
        assertThat(resposta).bodyJson().extractingPath("$.code").isEqualTo("USERNAME_EM_USO");
        assertThat(usernameNoBanco()).isEqualTo("danielribeiro");
    }

    @Test
    @Transactional
    void mandarOProprioUsernameNaoEhConflito() {
        assertThat(patchPerfil("{\"username\": \"danielribeiro\"}")).hasStatusOk();
    }

    @Test
    @Transactional
    void usernameForaDoFormatoRetorna400() {
        var resposta = patchPerfil("{\"username\": \"Daniel Ribeiro!\"}");

        assertThat(resposta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(resposta).bodyJson().extractingPath("$.code").isEqualTo("PERFIL_INVALIDO");
    }

    @Test
    @Transactional
    void nomeEmBrancoRetorna400() {
        var resposta = patchPerfil("{\"nome\": \"   \"}");

        assertThat(resposta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(resposta).bodyJson().extractingPath("$.code").isEqualTo("PERFIL_INVALIDO");
    }
}
