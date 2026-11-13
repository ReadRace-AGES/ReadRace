package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.dto.request.CriarComunidadeRequest;
import com.readrace.api.model.MembroComunidade;
import com.readrace.api.model.UsuarioId;
import com.readrace.api.repository.MembroComunidadeRepository;
import com.readrace.api.service.UsuarioAtual;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CriarComunidadeControllerIT {
    private static final UUID CRIADOR = UUID.fromString("00000000-0000-0000-0000-000000000001");
    @Autowired MockMvcTester mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired ObjectMapper json;
    @MockitoSpyBean UsuarioAtual usuarioAtual;
    @MockitoSpyBean MembroComunidadeRepository membros;

    @Test
    void cria_normaliza_e_aparece_no_feed_busca_e_perfil_com_administrador_autenticado() {
        var resposta =
                mvc.post()
                        .uri("/api/comunidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                    {"nome":"  Entre páginas e cafés  ","descricao":"  Conversas sobre livros.  ",
                     "imagemUrl":" https://example.com/capa.jpg ",
                     "criadorId":"00000000-0000-0000-0000-000000000002","cargo":"membro"}
                    """)
                        .exchange();
        assertThat(resposta).hasStatus(HttpStatus.CREATED);
        UUID id =
                UUID.fromString(
                        json.readTree(resposta.getResponse().getContentAsByteArray())
                                .get("id")
                                .asString());
        assertThat(resposta.getResponse().getHeader("Location"))
                .isEqualTo("/api/comunidades/" + id);
        assertThat(resposta)
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsEntry("nome", "Entre páginas e cafés")
                .containsEntry("descricao", "Conversas sobre livros.")
                .containsEntry("imagemUrl", "https://example.com/capa.jpg")
                .containsEntry("seguidores", 1)
                .containsEntry("sigo", true);
        entityManager.flush();
        assertThat(
                        jdbc.queryForList(
                                "SELECT usuario_id, cargo_comunidade::text AS cargo FROM membro_comunidade WHERE comunidade_id = ?",
                                id))
                .containsExactly(Map.of("usuario_id", CRIADOR, "cargo", "administrador"));
        assertThat(mvc.get().uri("/api/feed/comunidades"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.comunidades[*].id")
                .asArray()
                .contains(id.toString());
        assertThat(
                        mvc.get()
                                .uri("/api/busca")
                                .param("tipo", "comunidades")
                                .param("q", "Entre páginas"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.itens[*].id")
                .asArray()
                .contains(id.toString());
        assertThat(mvc.get().uri("/api/comunidades/" + id))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.descricao")
                .isEqualTo("Conversas sobre livros.");
        assertThat(mvc.delete().uri("/api/comunidades/" + id + "/membros/eu"))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void aceita_campos_opcionais_ausentes_e_limites_exatos() {
        assertThat(
                        mvc.post()
                                .uri("/api/comunidades")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nome\":\"Uma comunidade\"}"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.imagemUrl")
                .isNull();
        assertThat(
                        mvc.post()
                                .uri("/api/comunidades")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                new CriarComunidadeRequest(
                                                        "a".repeat(120), "b".repeat(2000), "  "))))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.descricao")
                .isEqualTo("b".repeat(2000));
    }

    static Stream<CriarComunidadeRequest> invalidos() {
        return Stream.of(
                new CriarComunidadeRequest(null, null, null),
                new CriarComunidadeRequest("  \n\t ", null, null),
                new CriarComunidadeRequest("a".repeat(121), null, null),
                new CriarComunidadeRequest("Nome\nOutra linha", null, null),
                new CriarComunidadeRequest("Nome", "a".repeat(2001), null),
                new CriarComunidadeRequest("Nome", null, "http://example.com/capa.jpg"),
                new CriarComunidadeRequest("Nome", null, "javascript:alert(1)"),
                new CriarComunidadeRequest("Nome", null, "data:image/png;base64,abcd"),
                new CriarComunidadeRequest(
                        "Nome", null, "https://usuario:senha@example.com/capa.jpg"),
                new CriarComunidadeRequest("Nome", null, "https:///capa.jpg"),
                new CriarComunidadeRequest("Nome", null, "https://example.com:99999/capa.jpg"),
                new CriarComunidadeRequest(
                        "Nome", null, "https://example.com/" + "a".repeat(2048)));
    }

    @ParameterizedTest
    @MethodSource("invalidos")
    void dados_invalidos_nao_criam_comunidade_ou_membro(CriarComunidadeRequest request) {
        long antes = total("comunidade");
        long membrosAntes = total("membro_comunidade");
        assertThat(
                        mvc.post()
                                .uri("/api/comunidades")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json.writeValueAsString(request)))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(total("comunidade")).isEqualTo(antes);
        assertThat(total("membro_comunidade")).isEqualTo(membrosAntes);
    }

    @Test
    void usuario_excluido_nao_pode_criar() {
        jdbc.update("UPDATE usuario SET excluido_em = now() WHERE id = ?", CRIADOR);
        assertThat(
                        mvc.post()
                                .uri("/api/comunidades")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nome\":\"Leitores\"}"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void reverte_comunidade_ja_inserida_quando_vinculo_do_administrador_falha() {
        long antes = total("comunidade");
        long membrosAntes = total("membro_comunidade");
        doAnswer(
                        invocacao -> {
                            entityManager.flush();
                            throw new IllegalStateException(
                                    "Falha simulada ao vincular administrador");
                        })
                .when(membros)
                .save(any(MembroComunidade.class));
        assertThat(
                        mvc.post()
                                .uri("/api/comunidades")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nome\":\"Rollback da comunidade\"}"))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(total("comunidade")).isEqualTo(antes);
        assertThat(total("membro_comunidade")).isEqualTo(membrosAntes);
    }

    @Test
    void usuario_inexistente_nao_deixa_comunidade_orfa() {
        long antes = total("comunidade");
        doReturn(new UsuarioId(UUID.randomUUID())).when(usuarioAtual).idDoUsuarioAtual();
        assertThat(
                        mvc.post()
                                .uri("/api/comunidades")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nome\":\"Sem criador\"}"))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(total("comunidade")).isEqualTo(antes);
    }

    private long total(String tabela) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabela, Long.class);
    }
}
