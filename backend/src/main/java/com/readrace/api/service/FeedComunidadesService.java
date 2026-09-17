package com.readrace.api.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.FeedComunidadesResponse;
import com.readrace.api.exception.RecursoNaoEncontradoException;
import com.readrace.api.model.ClubeDoLivro;
import com.readrace.api.model.Comunidade;
import com.readrace.api.model.Livro;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.ContagemMembros;
import com.readrace.api.repository.MembroClubeRepository;
import com.readrace.api.repository.MembroComunidadeRepository;
import com.readrace.api.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class FeedComunidadesService {

    private final MembroClubeRepository membroClubeRepository;
    private final MembroComunidadeRepository membroComunidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioAtualDeSeed usuarioAtual;

    public FeedComunidadesService(
            MembroClubeRepository membroClubeRepository,
            MembroComunidadeRepository membroComunidadeRepository,
            UsuarioRepository usuarioRepository,
            UsuarioAtualDeSeed usuarioAtual) {
        this.membroClubeRepository = membroClubeRepository;
        this.membroComunidadeRepository = membroComunidadeRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioAtual = usuarioAtual;
    }

    public FeedComunidadesResponse buscar() {
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();

        Usuario usuario =
                usuarioRepository
                        .findByIdAndExcluidoEmIsNull(usuarioId)
                        .orElseThrow(
                                () ->
                                        new RecursoNaoEncontradoException(
                                                "Usuário atual não encontrado."));

        Map<UUID, Long> membrosPorClube =
                contagemPorId(membroClubeRepository.contarMembrosPorClube());
        Map<UUID, Long> membrosPorComunidade =
                contagemPorId(membroComunidadeRepository.contarMembrosPorComunidade());

        List<FeedComunidadesResponse.Clube> clubes =
                membroClubeRepository.clubesDoUsuario(usuarioId).stream()
                        .map(clube -> paraClube(clube, membrosPorClube))
                        .toList();

        List<FeedComunidadesResponse.Comunidade> comunidades =
                membroComunidadeRepository.comunidadesDoUsuario(usuarioId).stream()
                        .map(comunidade -> paraComunidade(comunidade, membrosPorComunidade))
                        .toList();

        return new FeedComunidadesResponse(
                new FeedComunidadesResponse.Usuario(
                        usuario.getNome(), usuario.getDiasConsecutivos()),
                clubes,
                comunidades);
    }

    private static Map<UUID, Long> contagemPorId(List<ContagemMembros> contagens) {
        return contagens.stream()
                .collect(Collectors.toMap(ContagemMembros::getId, ContagemMembros::getTotal));
    }

    private static FeedComunidadesResponse.Clube paraClube(
            ClubeDoLivro clube, Map<UUID, Long> membrosPorClube) {
        Livro livro = clube.getLivro();
        String autor =
                livro.getLivroAutores().isEmpty()
                        ? null
                        : livro.getLivroAutores().stream()
                                .map(vinculo -> vinculo.getAutor().getNome())
                                .collect(Collectors.joining(", "));

        return new FeedComunidadesResponse.Clube(
                clube.getId(),
                clube.getNome(),
                clube.getCapaUrl(),
                new FeedComunidadesResponse.LivroAtual(livro.getTitulo(), autor),
                membrosPorClube.getOrDefault(clube.getId(), 0L));
    }

    private static FeedComunidadesResponse.Comunidade paraComunidade(
            Comunidade comunidade, Map<UUID, Long> membrosPorComunidade) {
        return new FeedComunidadesResponse.Comunidade(
                comunidade.getId(),
                comunidade.getNome(),
                comunidade.getImagemUrl(),
                membrosPorComunidade.getOrDefault(comunidade.getId(), 0L));
    }
}
