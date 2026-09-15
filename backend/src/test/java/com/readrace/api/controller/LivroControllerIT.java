package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LivroControllerIT {
    private static final String DOM_CASMURRO = "/api/livros/30000000-0000-0000-0000-000000000001";
    @Autowired private MockMvcTester mvc;

    @Test
    void deve_ler_detalhe_em_transacao_somente_leitura() {
        assertThat(mvc.get().uri(DOM_CASMURRO))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.livro.autor")
                .isEqualTo("Machado de Assis");
        assertThat(mvc.get().uri(DOM_CASMURRO))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.progresso.percentual")
                .isEqualTo(57);
        assertThat(mvc.get().uri(DOM_CASMURRO))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.progresso.paginaMaximaAlcancada")
                .isEqualTo(145);
    }

    @Test
    void deve_ignorar_usuario_informado_pelo_cliente() {
        assertThat(
                        mvc.get()
                                .uri(DOM_CASMURRO)
                                .param("userId", "00000000-0000-0000-0000-000000000002"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.progresso.paginaAtual")
                .isEqualTo(145);
    }

    @Test
    void deve_retornar_progresso_nulo_para_livro_fora_da_biblioteca() {
        assertThat(mvc.get().uri("/api/livros/30000000-0000-0000-0000-000000000013"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.progresso")
                .isNull();
    }

    @Test
    void deve_retornar_404_no_envelope_padrao() {
        assertThat(mvc.get().uri("/api/livros/30000000-0000-0000-0000-000000009999"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("LIVRO_NAO_ENCONTRADO");
    }

    @Test
    @Transactional
    @Sql(
            statements =
                    """
            INSERT INTO autor (id, nome) VALUES ('20000000-0000-0000-0000-000000009999', 'Coautor de teste');
            INSERT INTO livro_autor (livro_id, autor_id, ordem)
            VALUES ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000009999', 2);
            """)
    void deve_respeitar_a_ordem_dos_autores_da_relacao() {
        assertThat(mvc.get().uri(DOM_CASMURRO))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.livro.autor")
                .isEqualTo("Machado de Assis, Coautor de teste");
    }
}
