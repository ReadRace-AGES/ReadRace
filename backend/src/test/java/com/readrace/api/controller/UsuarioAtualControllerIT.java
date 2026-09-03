package com.readrace.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.readrace.api.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GET /api/me")
class UsuarioAtualControllerIT {
    private static final String ID_DE_SEED = "00000000-0000-0000-0000-000000000001";

    @Autowired private MockMvcTester mvc;

    @Test
    void deve_devolver_o_usuario_de_seed_sem_receber_id_do_cliente() {
        assertThat(mvc.get().uri("/api/me"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(ID_DE_SEED);
    }

    @Test
    void deve_serializar_o_id_como_string_e_nao_como_objeto_aninhado() {
        assertThat(mvc.get().uri("/api/me"))
                .bodyJson()
                .extractingPath("$.id")
                .asString()
                .isNotNull();
    }

    @Test
    void deve_devolver_o_mesmo_usuario_em_requisicoes_diferentes() {
        assertThat(mvc.get().uri("/api/me"))
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(ID_DE_SEED);
        assertThat(mvc.get().uri("/api/me"))
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(ID_DE_SEED);
    }

    @Test
    void deve_ignorar_o_id_que_o_cliente_tentar_passar_na_query() {
        assertThat(mvc.get().uri("/api/me?userId=99999999-9999-9999-9999-999999999999"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(ID_DE_SEED);
    }

    @Test
    void deve_recusar_o_verbo_errado_no_formato_padrao_de_erro() {
        assertThat(mvc.post().uri("/api/me"))
                .hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("METHOD_NOT_ALLOWED");
    }
}
