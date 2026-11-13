package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.JsonPath;
import com.readrace.api.model.ClubeDoLivro;
import com.readrace.api.model.Comunidade;
import com.readrace.api.model.Livro;
import com.readrace.api.model.SequenciaDeLeitura;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.ComunidadeRepository;
import com.readrace.api.repository.ContagemMembros;
import com.readrace.api.repository.MembroClubeRepository;
import com.readrace.api.repository.MembroComunidadeRepository;
import com.readrace.api.repository.UsuarioRepository;

import tools.jackson.databind.ObjectMapper;

class FeedComunidadesServiceTest {
    private final UUID eu = UUID.randomUUID();
    private final MembroClubeRepository membrosClube = mock(MembroClubeRepository.class);
    private final MembroComunidadeRepository membrosComunidade =
            mock(MembroComunidadeRepository.class);
    private final ComunidadeRepository comunidades = mock(ComunidadeRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final Usuario usuario = mock(Usuario.class);
    private final FeedComunidadesService service =
            new FeedComunidadesService(
                    membrosClube,
                    membrosComunidade,
                    usuarios,
                    new UsuarioAtualDeSeed(eu),
                    comunidades);

    @Test
    void preserva_campos_antigos_e_serializa_sugestoes_com_contagens_agrupadas() throws Exception {
        when(usuarios.findByIdAndExcluidoEmIsNull(eu)).thenReturn(Optional.of(usuario));
        when(usuario.getNome()).thenReturn("Leitor");
        when(usuario.getUltimaLeituraEm()).thenReturn(SequenciaDeLeitura.hoje());
        when(usuario.sequenciaExibida(SequenciaDeLeitura.hoje())).thenReturn(7);
        var clube = mock(ClubeDoLivro.class);
        UUID clubeId = UUID.randomUUID();
        when(clube.getId()).thenReturn(clubeId);
        when(clube.getNome()).thenReturn("Clube");
        when(clube.getCapaUrl()).thenReturn("capa-clube");
        var livro = mock(Livro.class);
        when(livro.getTitulo()).thenReturn("Livro");
        when(clube.getLivro()).thenReturn(livro);
        when(membrosClube.clubesDoUsuario(eu)).thenReturn(List.of(clube));
        var contagemClube = contagem(clubeId, 8);
        when(membrosClube.contarMembrosPorClube()).thenReturn(List.of(contagemClube));
        var minha = comunidade("Minha", "imagem-minha");
        var sugerida = comunidade("Sugerida", "imagem-sugerida");
        var semMembros = comunidade("Sem membros", null);
        when(membrosComunidade.comunidadesDoUsuario(eu)).thenReturn(List.of(minha));
        when(comunidades.sugeridasParaUsuario(eu)).thenReturn(List.of(sugerida, semMembros));
        var contagens = List.of(contagem(minha.getId(), 10), contagem(sugerida.getId(), 9));
        when(membrosComunidade.contarMembrosPorComunidade()).thenReturn(contagens);

        String json = new ObjectMapper().writeValueAsString(service.buscar());

        Map<String, Object> cabecalho = JsonPath.read(json, "$.usuario");
        assertThat(cabecalho)
                .containsOnlyKeys("nome", "sequenciaDias", "leuHoje", "diasAtePerder")
                .containsEntry("nome", "Leitor")
                .containsEntry("sequenciaDias", 7)
                .containsEntry("leuHoje", true)
                .containsEntry("diasAtePerder", null);
        Map<String, Object> antigo = JsonPath.read(json, "$.comunidades[0]");
        assertThat(antigo)
                .isEqualTo(
                        Map.of(
                                "id",
                                minha.getId().toString(),
                                "nome",
                                "Minha",
                                "capaUrl",
                                "imagem-minha",
                                "totalMembros",
                                10));
        Map<String, Object> novo = JsonPath.read(json, "$.comunidadesSugeridas[0]");
        assertThat(novo)
                .isEqualTo(
                        Map.of(
                                "id",
                                sugerida.getId().toString(),
                                "nome",
                                "Sugerida",
                                "imagemUrl",
                                "imagem-sugerida",
                                "membros",
                                9));
        Map<String, Object> vazio = JsonPath.read(json, "$.comunidadesSugeridas[1]");
        assertThat(vazio).containsEntry("membros", 0).containsEntry("imagemUrl", null);
        Map<String, Object> clubeJson = JsonPath.read(json, "$.clubes[0]");
        assertThat(clubeJson)
                .containsOnlyKeys("id", "nome", "capaUrl", "livroAtual", "totalMembros")
                .containsEntry("id", clubeId.toString())
                .containsEntry("nome", "Clube")
                .containsEntry("capaUrl", "capa-clube")
                .containsEntry("totalMembros", 8);
        Map<String, Object> livroJson = JsonPath.read(json, "$.clubes[0].livroAtual");
        assertThat(livroJson)
                .containsOnlyKeys("titulo", "autor")
                .containsEntry("titulo", "Livro")
                .containsEntry("autor", null);
        verify(membrosComunidade).comunidadesDoUsuario(eu);
        verify(membrosComunidade).contarMembrosPorComunidade();
        verify(comunidades).sugeridasParaUsuario(eu);
        verifyNoMoreInteractions(membrosComunidade, comunidades);
    }

    @Test
    void ausencia_de_sugestoes_serializa_array_vazio() throws Exception {
        when(usuarios.findByIdAndExcluidoEmIsNull(eu)).thenReturn(Optional.of(usuario));
        String json = new ObjectMapper().writeValueAsString(service.buscar());
        assertThat(JsonPath.<List<?>>read(json, "$.comunidadesSugeridas")).isEmpty();
        assertThat(JsonPath.<List<?>>read(json, "$.comunidades")).isEmpty();
        assertThat(JsonPath.<List<?>>read(json, "$.clubes")).isEmpty();
    }

    private Comunidade comunidade(String nome, String imagem) {
        var comunidade = mock(Comunidade.class);
        when(comunidade.getId()).thenReturn(UUID.randomUUID());
        when(comunidade.getNome()).thenReturn(nome);
        when(comunidade.getImagemUrl()).thenReturn(imagem);
        return comunidade;
    }

    private ContagemMembros contagem(UUID id, long total) {
        var contagem = mock(ContagemMembros.class);
        when(contagem.getId()).thenReturn(id);
        when(contagem.getTotal()).thenReturn(total);
        return contagem;
    }
}
