package com.readrace.api.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.request.MensagemClubeRequest;
import com.readrace.api.dto.response.MensagemClubeResponse;
import com.readrace.api.dto.response.MensagensClubeResponse;
import com.readrace.api.exception.ClubeNaoEncontradoException;
import com.readrace.api.exception.MensagemInvalidaException;
import com.readrace.api.exception.SoMembroNoChatException;
import com.readrace.api.model.ClubeDoLivro;
import com.readrace.api.model.MensagemClube;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.ClubeDoLivroRepository;
import com.readrace.api.repository.MembroClubeRepository;
import com.readrace.api.repository.MensagemClubeRepository;
import com.readrace.api.repository.UsuarioRepository;

@Service
public class MensagemClubeService {

    private static final int LIMITE_MENSAGENS = 100;

    private final MensagemClubeRepository mensagemRepository;
    private final ClubeDoLivroRepository clubeRepository;
    private final MembroClubeRepository membroClubeRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioAtual usuarioAtual;

    public MensagemClubeService(
            MensagemClubeRepository mensagemRepository,
            ClubeDoLivroRepository clubeRepository,
            MembroClubeRepository membroClubeRepository,
            UsuarioRepository usuarioRepository,
            UsuarioAtual usuarioAtual) {
        this.mensagemRepository = mensagemRepository;
        this.clubeRepository = clubeRepository;
        this.membroClubeRepository = membroClubeRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional(readOnly = true)
    public MensagensClubeResponse buscar(UUID clubeId, OffsetDateTime depois) {
        ClubeDoLivro clube = buscarClube(clubeId);
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        verificarMembro(clubeId, usuarioId);

        List<MensagemClube> mensagens;

        if (depois == null) {
            mensagens =
                    new ArrayList<>(
                            mensagemRepository.findByClube_IdOrderByEnviadaEmDesc(
                                    clube.getId(),
                                    PageRequest.of(0, LIMITE_MENSAGENS)));

            Collections.reverse(mensagens);
        } else {
            mensagens =
                    mensagemRepository
                            .findByClube_IdAndEnviadaEmGreaterThanOrderByEnviadaEmAsc(
                                    clube.getId(),
                                    depois);
        }

        List<MensagemClubeResponse> respostas =
                mensagens.stream()
                        .map(mensagem -> paraResponse(mensagem, usuarioId))
                        .toList();

        return new MensagensClubeResponse(respostas);
    }

    @Transactional
    public MensagemClubeResponse enviar(UUID clubeId, MensagemClubeRequest request) {
        ClubeDoLivro clube = buscarClube(clubeId);
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        verificarMembro(clubeId, usuarioId);
        validarMensagem(request);

        Usuario autor =
                usuarioRepository
                        .findById(usuarioId)
                        .orElseThrow(SoMembroNoChatException::new);

        MensagemClube mensagem =
                new MensagemClube(
                        UUID.randomUUID(),
                        clube,
                        autor,
                        request.texto(),
                        OffsetDateTime.now());

        MensagemClube salva = mensagemRepository.save(mensagem);

        return paraResponse(salva, usuarioId);
    }

    private ClubeDoLivro buscarClube(UUID clubeId) {
        return clubeRepository
                .findByIdAndExcluidoEmIsNull(clubeId)
                .orElseThrow(ClubeNaoEncontradoException::new);
    }

    private void verificarMembro(UUID clubeId, UUID usuarioId) {
        if (membroClubeRepository
                .findByClube_IdAndUsuarioId(clubeId, usuarioId)
                .isEmpty()) {
            throw new SoMembroNoChatException();
        }
    }

    private void validarMensagem(MensagemClubeRequest request) {
        if (request == null
                || request.texto() == null
                || request.texto().isBlank()
                || request.texto().length() > 1000) {
            throw new MensagemInvalidaException();
        }
    }

    private MensagemClubeResponse paraResponse(
            MensagemClube mensagem,
            UUID usuarioId) {

        Usuario autor = mensagem.getAutor();

        return new MensagemClubeResponse(
                mensagem.getId(),
                mensagem.getTexto(),
                mensagem.getEnviadaEm(),
                autor.getId().equals(usuarioId),
                new MensagemClubeResponse.Autor(
                        autor.getId(),
                        autor.getNome(),
                        autor.getAvatarUrl()));
    }
}