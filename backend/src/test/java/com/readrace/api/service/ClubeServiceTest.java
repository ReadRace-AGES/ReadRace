package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import com.readrace.api.model.Cargo;
import com.readrace.api.model.ClubeDoLivro;
import com.readrace.api.model.Livro;
import com.readrace.api.model.MembroClube;
import com.readrace.api.model.UsuarioId;
import com.readrace.api.repository.ClubeDoLivroRepository;
import com.readrace.api.repository.LinhaRankingClube;
import com.readrace.api.repository.MembroClubeRepository;
import com.readrace.api.repository.QuizRepository;

@ExtendWith(MockitoExtension.class)
class ClubeServiceTest {
    @Mock private ClubeDoLivroRepository clubeRepository;
    @Mock private MembroClubeRepository membroClubeRepository;
    @Mock private UsuarioAtual usuarioAtual;
    @Mock private QuizRepository quizRepository;
    @InjectMocks private ClubeService service;

    private final UUID clubeId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();

    @Test
    void pagina_do_clube_deve_preservar_posicoes_empatadas_da_consulta() {
        ClubeDoLivro clube = clubeComMembroAtual();
        Livro livro = mock(Livro.class);
        when(clube.getLivro()).thenReturn(livro);
        when(livro.getLivroAutores()).thenReturn(List.of());
        List<LinhaRankingClube> linhas = List.of(linha(1, 900), linha(1, 900), linha(3, 800));
        when(membroClubeRepository.rankingDoClube(clubeId, PageRequest.of(0, 7)))
                .thenReturn(linhas);

        assertThat(service.buscar(clubeId).ranking())
                .extracting(r -> r.posicao())
                .containsExactly(1, 1, 3);
    }

    @Test
    void pagina_posterior_deve_preservar_posicao_global_do_usuario_e_cursor() {
        clubeComMembroAtual();
        var pagina = PageRequest.of(1, 50);
        LinhaRankingClube usuario = linha(49, 500);
        when(usuario.getUsuarioId()).thenReturn(usuarioId);
        var linhas =
                new SliceImpl<>(List.of(usuario, linha(49, 500), linha(53, 100)), pagina, false);
        when(membroClubeRepository.paginaDoRanking(clubeId, pagina)).thenReturn(linhas);

        var resposta = service.buscarRanking(clubeId, "1");

        assertThat(resposta.ranking()).extracting(r -> r.posicao()).containsExactly(49, 49, 53);
        assertThat(resposta.ranking().getFirst().usuario().id()).isEqualTo(usuarioId);
        assertThat(resposta.proximoCursor()).isNull();
        verify(membroClubeRepository).paginaDoRanking(clubeId, pagina);
    }

    private ClubeDoLivro clubeComMembroAtual() {
        ClubeDoLivro clube = mock(ClubeDoLivro.class);
        MembroClube membro = mock(MembroClube.class);
        when(clubeRepository.findByIdAndExcluidoEmIsNull(clubeId)).thenReturn(Optional.of(clube));
        when(usuarioAtual.idDoUsuarioAtual()).thenReturn(new UsuarioId(usuarioId));
        when(membroClubeRepository.findByClube_IdAndUsuarioId(clubeId, usuarioId))
                .thenReturn(Optional.of(membro));
        when(membro.getCargoClube()).thenReturn(Cargo.MEMBRO);
        return clube;
    }

    private LinhaRankingClube linha(long posicao, int pontos) {
        LinhaRankingClube linha = mock(LinhaRankingClube.class);
        when(linha.getPosicao()).thenReturn(posicao);
        when(linha.getPontos()).thenReturn(pontos);
        return linha;
    }
}
