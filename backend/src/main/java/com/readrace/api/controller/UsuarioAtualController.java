package com.readrace.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.readrace.api.dto.response.UsuarioAtualResponse;
import com.readrace.api.service.UsuarioAtualService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Quem é o usuário da requisição.
 *
 * <pre>
 * GET /api/me   200 {"id":"00000000-0000-0000-0000-000000000001"}
 * </pre>
 *
 * <p><b>Repare no que este método NÃO tem:</b> nenhum parâmetro. Sem {@code @PathVariable}, sem
 * {@code @RequestParam}, sem corpo. O cliente não diz quem é — o backend resolve. Adicionar um
 * parâmetro de id aqui, ou em qualquer outro endpoint, reabre a falha de IDOR que a issue #10
 * fecha, e derruba o teste {@code
 * UsuarioAtualControllerIT.deve_ignorar_o_id_que_o_cliente_tentar_passar_na_query}.
 */
@RestController
@Tag(name = "Usuário atual", description = "Dados de quem está usando o app")
public class UsuarioAtualController {

    private final UsuarioAtualService usuarioAtualService;

    public UsuarioAtualController(UsuarioAtualService usuarioAtualService) {
        this.usuarioAtualService = usuarioAtualService;
    }

    @GetMapping("/api/me")
    @Operation(summary = "Devolve o usuário atual, resolvido pelo backend")
    public UsuarioAtualResponse eu() {
        return usuarioAtualService.buscar();
    }
}
