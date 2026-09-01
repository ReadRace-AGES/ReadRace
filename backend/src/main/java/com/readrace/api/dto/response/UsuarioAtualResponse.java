package com.readrace.api.dto.response;

import java.util.UUID;

import com.readrace.api.model.UsuarioId;

/**
 * O que o {@code GET /api/me} devolve.
 *
 * <p>Só o id, porque a tabela de usuário ainda não existe — schema e seed são as issues #12 e #13,
 * que rodam em paralelo e estão fora do escopo da #10. Quando existirem, este record ganha nome,
 * avatar, nível e XP, e a URL continua a mesma.
 *
 * <p>O campo é {@link UUID} cru, não {@link UsuarioId}: DTO é o formato do fio. Se o value object
 * fosse serializado direto, o JSON sairia {@code {"id":{"valor":"..."}}} e o mobile teria que
 * conhecer um detalhe interno do domínio do backend.
 */
public record UsuarioAtualResponse(UUID id) {

    public static UsuarioAtualResponse de(UsuarioId id) {
        return new UsuarioAtualResponse(id.valor());
    }
}
