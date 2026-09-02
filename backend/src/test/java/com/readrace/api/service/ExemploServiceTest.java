package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.readrace.api.dto.request.ExemploRequest;
import com.readrace.api.dto.response.ExemploResponse;
import com.readrace.api.exception.RecursoNaoEncontradoException;
import com.readrace.api.model.Exemplo;
import com.readrace.api.repository.ExemploRepository;

/**
 * Teste unitário de service: sem contexto Spring, sem banco, sem Docker. Roda em milissegundos e
 * cobre a regra — quem cobre a integração é o {@code ExemploControllerIT}.
 *
 * <p>Este é o formato padrão de teste de service do projeto. Copie daqui.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExemploService")
class ExemploServiceTest {

    @Mock private ExemploRepository exemploRepository;

    @InjectMocks private ExemploService exemploService;

    @Test
    void deve_devolver_lista_vazia_quando_nao_houver_exemplo() {
        when(exemploRepository.findAll()).thenReturn(List.of());

        assertThat(exemploService.listar()).isEmpty();
    }

    @Test
    void deve_converter_entidade_para_dto_ao_listar() {
        when(exemploRepository.findAll())
                .thenReturn(List.of(new Exemplo("Dom Casmurro", "Machado de Assis")));

        List<ExemploResponse> resultado = exemploService.listar();

        assertThat(resultado)
                .singleElement()
                .extracting(ExemploResponse::nome, ExemploResponse::descricao)
                .containsExactly("Dom Casmurro", "Machado de Assis");
    }

    @Test
    void deve_lancar_excecao_quando_buscar_id_inexistente() {
        when(exemploRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exemploService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Exemplo 99 não encontrado");
    }

    @Test
    void deve_salvar_e_devolver_dto_ao_criar() {
        Exemplo salvo = new Exemplo("Grande Sertão", "Guimarães Rosa");
        when(exemploRepository.save(any(Exemplo.class))).thenReturn(salvo);

        ExemploResponse resultado =
                exemploService.criar(new ExemploRequest("Grande Sertão", "Guimarães Rosa"));

        assertThat(resultado.nome()).isEqualTo("Grande Sertão");
        verify(exemploRepository, times(1)).save(any(Exemplo.class));
    }

    /**
     * O service atualiza a entidade e NÃO chama save: dentro da transação o Hibernate detecta a
     * mudança sozinho (dirty checking). Se alguém "consertar" isso adicionando um save, este teste
     * quebra — que é exatamente o ponto.
     */
    @Test
    void nao_deve_chamar_save_ao_atualizar() {
        Exemplo existente = new Exemplo("nome antigo", "descrição antiga");
        when(exemploRepository.findById(1L)).thenReturn(Optional.of(existente));

        ExemploResponse resultado =
                exemploService.atualizar(1L, new ExemploRequest("nome novo", "descrição nova"));

        assertThat(resultado.nome()).isEqualTo("nome novo");
        assertThat(existente.getDescricao()).isEqualTo("descrição nova");
        verify(exemploRepository, never()).save(any(Exemplo.class));
    }

    @Test
    void deve_lancar_excecao_ao_excluir_id_inexistente() {
        when(exemploRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exemploService.excluir(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(exemploRepository, never()).delete(any(Exemplo.class));
    }
}
