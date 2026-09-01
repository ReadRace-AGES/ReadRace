package com.readrace.api.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.readrace.api.model.UsuarioId;

/**
 * A implementação da Sprint 1: sem autenticação, o usuário atual é sempre o mesmo.
 *
 * <p>O id vem do {@code application.yaml} e não de uma constante no código para que o valor possa
 * acompanhar o seed do banco (issue #13) sem recompilar, e para que cada ambiente aponte para o
 * próprio usuário de teste.
 *
 * <p><b>Esta classe é descartável.</b> Quando o módulo de autenticação entrar, ela é apagada e uma
 * implementação que lê o token toma o lugar. Por isso ela não faz nada além de devolver o valor: se
 * ganhar lógica, essa lógica se perde na troca.
 */
@Component
public class UsuarioAtualDeSeed implements UsuarioAtualProvider {

    private final UsuarioId id;

    public UsuarioAtualDeSeed(@Value("${readrace.usuario-seed.id}") UUID id) {
        this.id = new UsuarioId(id);
    }

    @Override
    public UsuarioId idDoUsuarioAtual() {
        return id;
    }
}
