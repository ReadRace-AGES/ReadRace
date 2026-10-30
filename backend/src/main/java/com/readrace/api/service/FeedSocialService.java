package com.readrace.api.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.FeedSocialResponse;
import com.readrace.api.model.Livro;
import com.readrace.api.model.SequenciaDeLeitura;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.LivroRepository;
import com.readrace.api.repository.RegistroLeituraRepository;
import com.readrace.api.repository.RegistroLeituraRepository.AtividadeSocial;
import com.readrace.api.repository.UsuarioRepository;

/**
 * Aba Social do Feed (#197, #204). Só lê: a lista sai do registro de leitura de quem o usuário atual
 * segue, sem tabela própria.
 */
@Service
@Transactional(readOnly = true)
public class FeedSocialService {

    static final Duration PERIODO = Duration.ofDays(30);
    static final int LIMITE = 50;

    static final String TIPO_CONCLUSAO = "conclusao";

    // A mesma regra do registro de leitura: 1 XP por página nova e o bônus ao terminar o livro.
    private static final int XP_POR_PAGINA = 1;

    private final RegistroLeituraRepository registroLeituraRepository;
    private final UsuarioRepository usuarioRepository;
    private final LivroRepository livroRepository;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;

    public FeedSocialService(
            RegistroLeituraRepository registroLeituraRepository,
            UsuarioRepository usuarioRepository,
            LivroRepository livroRepository,
            UsuarioAtual usuarioAtual,
            Clock clock) {
        this.registroLeituraRepository = registroLeituraRepository;
        this.usuarioRepository = usuarioRepository;
        this.livroRepository = livroRepository;
        this.usuarioAtual = usuarioAtual;
        this.clock = clock;
    }

    public FeedSocialResponse buscar() {
        UUID usuarioId = usuarioAtual.idDoUsuarioAtual().valor();
        Instant agora = clock.instant();

        List<AtividadeSocial> atividades =
                registroLeituraRepository.buscarAtividadesSociais(
                        usuarioId,
                        SequenciaDeLeitura.FUSO.getId(),
                        agora.minus(PERIODO),
                        agora,
                        LIMITE);

        if (atividades.isEmpty()) {
            return new FeedSocialResponse(List.of());
        }

        Map<UUID, Usuario> usuarios =
                usuarioRepository
                        .findAllById(
                                atividades.stream()
                                        .map(AtividadeSocial::getUsuarioId)
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(Usuario::getId, Function.identity()));
        Map<UUID, Livro> livros =
                livroRepository
                        .findByIdIn(
                                atividades.stream()
                                        .map(AtividadeSocial::getLivroId)
                                        .distinct()
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(Livro::getId, Function.identity()));

        LocalDate hoje = agora.atZone(SequenciaDeLeitura.FUSO).toLocalDate();

        return new FeedSocialResponse(
                atividades.stream()
                        .map(
                                atividade ->
                                        paraAtividade(
                                                atividade,
                                                usuarios.get(atividade.getUsuarioId()),
                                                livros.get(atividade.getLivroId()),
                                                hoje))
                        .toList());
    }

    private static FeedSocialResponse.Atividade paraAtividade(
            AtividadeSocial atividade, Usuario usuario, Livro livro, LocalDate hoje) {
        return new FeedSocialResponse.Atividade(
                atividade.getTipo(),
                new FeedSocialResponse.Usuario(
                        usuario.getId(),
                        usuario.getNome(),
                        usuario.getAvatarUrl(),
                        usuario.sequenciaExibida(hoje)),
                new FeedSocialResponse.Livro(
                        livro.getId(), livro.getTitulo(), autores(livro), livro.getTotalPaginas()),
                atividade.getPaginas(),
                xp(atividade),
                atividade.getEm().atZone(SequenciaDeLeitura.FUSO).toOffsetDateTime());
    }

    private static int xp(AtividadeSocial atividade) {
        return TIPO_CONCLUSAO.equals(atividade.getTipo())
                ? RegistrarProgressoService.XP_CONCLUSAO
                : atividade.getPaginas() * XP_POR_PAGINA;
    }

    private static String autores(Livro livro) {
        return livro.getLivroAutores().isEmpty()
                ? null
                : livro.getLivroAutores().stream()
                        .map(vinculo -> vinculo.getAutor().getNome())
                        .collect(Collectors.joining(", "));
    }
}
