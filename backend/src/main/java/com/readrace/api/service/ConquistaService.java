package com.readrace.api.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.model.Conquista;
import com.readrace.api.model.CriterioConquista;
import com.readrace.api.model.DesafioAmigo;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.ConquistaRepository;
import com.readrace.api.repository.UsuarioRepository;

@Service
public class ConquistaService {

    private final ConquistaRepository conquistaRepository;
    private final UsuarioRepository usuarioRepository;

    public ConquistaService(
            ConquistaRepository conquistaRepository, UsuarioRepository usuarioRepository) {
        this.conquistaRepository = conquistaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void avaliar(UUID usuarioId) {
        Optional<Usuario> usuario = usuarioRepository.buscarAtivoComLock(usuarioId);
        if (usuario.isEmpty()) {
            return;
        }

        conquistaRepository.flush();
        List<Conquista> pendentes =
                conquistaRepository.buscarPendentes(usuarioId, CriterioConquista.valores());
        Map<CriterioConquista, Long> medicoes = new EnumMap<>(CriterioConquista.class);
        int recompensa = 0;

        for (Conquista conquista : pendentes) {
            long medido =
                    medicoes.computeIfAbsent(
                            conquista.getCriterio(), criterio -> medir(criterio, usuarioId));

            if (conquista.foiAlcancada(medido)
                    && conquistaRepository.registrarSeAusente(
                                    UUID.randomUUID(), conquista.getId(), usuarioId)
                            > 0) {
                recompensa += conquista.getRecompensaXp();
            }
        }

        if (recompensa > 0) {
            usuario.get().receberXp(recompensa);
        }
    }

    @Transactional
    public void avaliarParticipantes(DesafioAmigo desafio) {
        Stream.of(desafio.getCriador().getId(), desafio.getOponente().getId())
                .sorted()
                .forEach(this::avaliar);
    }

    private long medir(CriterioConquista criterio, UUID usuarioId) {
        return switch (criterio) {
            case LIVROS_LIDOS -> conquistaRepository.contarLivrosLidos(usuarioId);
            case PAGINAS_LIDAS -> conquistaRepository.somarPaginasLidas(usuarioId);
            case DESAFIOS -> conquistaRepository.contarDesafiosFinalizados(usuarioId);
            case CLUBES -> conquistaRepository.contarClubes(usuarioId);
        };
    }
}
