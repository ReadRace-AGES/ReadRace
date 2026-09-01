package com.readrace.api.service;

import com.readrace.api.model.UsuarioId;

/**
 * Responde "quem está fazendo esta requisição?".
 *
 * <p><b>Esta interface é o motivo de nenhum endpoint aceitar {@code userId}.</b> Se o app mandasse
 * o id, qualquer cliente poderia trocar o valor e ler os dados de outra pessoa — a falha conhecida
 * como IDOR. Quem responde a pergunta é o backend, sempre.
 *
 * <p>É um <b>Strategy</b>: o comportamento fica atrás da interface e a implementação se troca sem
 * que ninguém que a consome perceba.
 *
 * <ul>
 *   <li><b>Sprint 1</b> — {@link UsuarioAtualDeSeed}: não há login, a resposta é uma constante
 *   <li><b>Quando a autenticação chegar</b> — uma implementação nova lê o id de dentro do token
 * </ul>
 *
 * <p>Nesse dia, troca-se o {@code @Component} e <b>nada mais no projeto muda</b>: nem controller,
 * nem service, nem a assinatura de um endpoint sequer, nem uma linha do mobile. É esse
 * desacoplamento que a issue #10 está comprando.
 */
public interface UsuarioAtualProvider {

    UsuarioId idDoUsuarioAtual();
}
