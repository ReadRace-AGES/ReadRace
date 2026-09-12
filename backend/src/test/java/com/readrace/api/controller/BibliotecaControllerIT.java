package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.readrace.api.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GET /api/biblioteca")
class BibliotecaControllerIT {
    // Ids do seed (V4): o usuário fixo tem 12 itens — 4 lendo, 5 lidos, 3 desejo, 3 favoritos.
    private static final String LIVRO_DOM_CASMURRO = "30000000-0000-0000-0000-000000000001";
    private static final String LIVRO_LIDO_MAIS_RECENTE = "30000000-0000-0000-0000-000000000007";

    @Autowired private MockMvcTester mvc;

    @Test
    void deve_devolver_exatamente_as_quatro_listas_e_nenhuma_recomendacao() {
        assertThat(mvc.get().uri("/api/biblioteca"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$")
                .asMap()
                .containsOnlyKeys("favoritos", "lendo", "desejo", "lidos");
    }

    @Test
    void deve_devolver_os_favoritos_do_usuario_fixo_sem_receber_id_do_cliente() {
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.favoritos.length()")
                .isEqualTo(3);
    }

    @Test
    void deve_repartir_os_itens_por_estado_de_leitura() {
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.lendo.length()")
                .isEqualTo(4);
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.desejo.length()")
                .isEqualTo(3);
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.lidos.length()")
                .isEqualTo(5);
    }

    @Test
    void deve_por_primeiro_o_livro_com_registro_de_leitura_mais_recente() {
        // Dom Casmurro foi adicionado há 30 dias, mas tem registro de leitura de ontem.
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.lendo[0].livroId")
                .isEqualTo(LIVRO_DOM_CASMURRO);
    }

    @Test
    void deve_ordenar_pela_data_de_adicao_quando_nao_houver_registro_de_leitura() {
        // Nenhum item lido tem registro; o adicionado há 50 dias vem antes dos de 60, 75, 90 e 120.
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.lidos[0].livroId")
                .isEqualTo(LIVRO_LIDO_MAIS_RECENTE);
    }

    @Test
    void favorito_deve_aparecer_tambem_na_lista_do_seu_estado() {
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.lendo[*].livroId")
                .asList()
                .contains(LIVRO_DOM_CASMURRO);
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.favoritos[*].livroId")
                .asList()
                .contains(LIVRO_DOM_CASMURRO);
    }

    @Test
    void deve_devolver_titulo_autor_e_capa_de_cada_livro() {
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.lendo[0]")
                .asMap()
                .containsEntry("titulo", "Dom Casmurro")
                .containsEntry("autor", "Machado de Assis")
                .containsKeys("capaUrl", "livroId");
    }

    @Test
    void deve_ignorar_o_id_que_o_cliente_tentar_passar_na_query() {
        assertThat(mvc.get().uri("/api/biblioteca?userId=99999999-9999-9999-9999-999999999999"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.favoritos.length()")
                .isEqualTo(3);
    }

    @Test
    @Sql(
            statements =
                    """
                    UPDATE item_biblioteca
                    SET status_leitura = 'lido'
                    WHERE usuario_id = '00000000-0000-0000-0000-000000000001'
                      AND status_leitura = 'desejo';
                    """)
    @Sql(
            statements =
                    """
                    UPDATE item_biblioteca
                    SET status_leitura = 'desejo'
                    WHERE id IN (
                        '40000000-0000-0000-0000-000000000005',
                        '40000000-0000-0000-0000-000000000008',
                        '40000000-0000-0000-0000-000000000011'
                    );
                    """,
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void deve_devolver_lista_vazia_e_nao_erro_quando_nao_houver_item_no_estado() {
        assertThat(mvc.get().uri("/api/biblioteca"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.desejo")
                .isEqualTo(List.of());
        assertThat(mvc.get().uri("/api/biblioteca"))
                .bodyJson()
                .extractingPath("$.lidos.length()")
                .isEqualTo(8);
    }

    @Test
    void deve_recusar_o_verbo_errado_no_formato_padrao_de_erro() {
        assertThat(mvc.post().uri("/api/biblioteca"))
                .hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("METHOD_NOT_ALLOWED");
    }
}
