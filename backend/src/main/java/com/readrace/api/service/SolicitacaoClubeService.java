package com.readrace.api.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.SolicitacaoClubeResponse;
import com.readrace.api.exception.ClubeNaoEncontradoException;
import com.readrace.api.exception.JaEMembroException;
import com.readrace.api.exception.SoLiderCriaQuizException;
import com.readrace.api.exception.SolicitacaoJaRespondidaException;
import com.readrace.api.exception.SolicitacaoNaoEncontradaException;
import com.readrace.api.exception.UsuarioNaoEncontradoException;
import com.readrace.api.model.Cargo;
import com.readrace.api.model.MembroClube;
import com.readrace.api.model.SituacaoSolicitacaoClube;
import com.readrace.api.model.SolicitacaoEntradaClube;
import com.readrace.api.repository.ClubeDoLivroRepository;
import com.readrace.api.repository.MembroClubeRepository;
import com.readrace.api.repository.SolicitacaoEntradaClubeRepository;
import com.readrace.api.repository.UsuarioRepository;

/**
 * Serializa no clube, depois no pedido e, no aceite, na associação existente e no solicitante. O
 * PESSIMISTIC_WRITE dos repositories existentes gera FOR NO KEY UPDATE neste Hibernate, compatível
 * com o KEY SHARE das FKs, inclusive quando os administradores são solicitantes cruzados. Não há
 * lock explícito no usuário do administrador.
 */
@Service
@Transactional
public class SolicitacaoClubeService {
    private final ClubeDoLivroRepository clubes;
    private final MembroClubeRepository membros;
    private final SolicitacaoEntradaClubeRepository solicitacoes;
    private final UsuarioRepository usuarios;
    private final UsuarioAtual usuarioAtual;
    private final ConquistaService conquistas;
    private final Clock clock;
    private final EntityManager entityManager;

    public SolicitacaoClubeService(
            ClubeDoLivroRepository clubes,
            MembroClubeRepository membros,
            SolicitacaoEntradaClubeRepository solicitacoes,
            UsuarioRepository usuarios,
            UsuarioAtual usuarioAtual,
            ConquistaService conquistas,
            Clock clock,
            EntityManager entityManager) {
        this.clubes = clubes;
        this.membros = membros;
        this.solicitacoes = solicitacoes;
        this.usuarios = usuarios;
        this.usuarioAtual = usuarioAtual;
        this.conquistas = conquistas;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    public SolicitacaoClubeResponse solicitar(UUID clubeId) {
        // O provisionamento Cognito termina antes de qualquer lock de negócio.
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();
        clubes.buscarComLock(clubeId).orElseThrow(ClubeNaoEncontradoException::new);
        if (membros.findByClube_IdAndUsuarioId(clubeId, usuarioId).isPresent()) {
            throw new JaEMembroException();
        }
        // O lock do clube serializa inclusive o caso em que ainda não há pedido para travar.
        var pedido =
                solicitacoes
                        .buscarPendente(clubeId, usuarioId)
                        .orElseGet(
                                () ->
                                        solicitacoes.save(
                                                new SolicitacaoEntradaClube(
                                                        clubeId,
                                                        usuarioId,
                                                        OffsetDateTime.now(clock))));
        return SolicitacaoClubeResponse.de(pedido);
    }

    public SolicitacaoClubeResponse aceitar(UUID clubeId, UUID id) {
        return responder(clubeId, id, SituacaoSolicitacaoClube.ACEITA);
    }

    public SolicitacaoClubeResponse recusar(UUID clubeId, UUID id) {
        return responder(clubeId, id, SituacaoSolicitacaoClube.RECUSADA);
    }

    private SolicitacaoClubeResponse responder(
            UUID clubeId, UUID id, SituacaoSolicitacaoClube resposta) {
        UUID administradorId = usuarioAtual.idDoUsuarioAtual().valor();
        var clube = clubes.buscarComLock(clubeId).orElseThrow(ClubeNaoEncontradoException::new);
        // Saída e transferência usam o mesmo lock do clube: consultar o cargo só depois dele.
        if (membros.findByClube_IdAndUsuarioId(clubeId, administradorId)
                .filter(m -> m.getCargoClube() == Cargo.ADMINISTRADOR)
                .isEmpty()) {
            throw new SoLiderCriaQuizException(
                    "Só o administrador do clube pode responder às solicitações.");
        }
        var pedido =
                solicitacoes
                        .buscarComLock(id, clubeId)
                        .orElseThrow(SolicitacaoNaoEncontradaException::new);
        if (pedido.getSituacao() != SituacaoSolicitacaoClube.PENDENTE) {
            throw new SolicitacaoJaRespondidaException();
        }
        if (resposta == SituacaoSolicitacaoClube.ACEITA) {
            // Se já associado, respeitar a mesma ordem do quiz/leitura: associação -> usuário.
            var membro = membros.buscarComLock(clubeId, pedido.getUsuarioId());
            var usuario =
                    usuarios.buscarAtivoComLock(pedido.getUsuarioId())
                            .orElseThrow(UsuarioNaoEncontradoException::new);
            // A consulta com lock não renova uma entidade já gerenciada. Reler enquanto temos
            // o lock preserva XP confirmado por quiz/leitura durante a espera, inclusive quando
            // a identidade Cognito já havia carregado este usuário antes dos locks de negócio.
            entityManager.refresh(usuario);
            if (membro.isEmpty()) {
                membros.save(new MembroClube(clube, pedido.getUsuarioId(), Cargo.MEMBRO));
            }
        }
        pedido.responder(resposta, OffsetDateTime.now(clock), administradorId);
        if (resposta == SituacaoSolicitacaoClube.ACEITA) {
            // A avaliação faz flush: associação, resposta, conquista e XP são atômicos.
            conquistas.avaliar(pedido.getUsuarioId());
        }
        return SolicitacaoClubeResponse.de(pedido);
    }
}
