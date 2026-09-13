package com.readrace.api.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.request.RegistrarProgressoRequest;
import com.readrace.api.dto.response.ProgressoLeituraResponse;
import com.readrace.api.exception.LivroNaoEncontradoException;
import com.readrace.api.exception.PaginaInvalidaException;
import com.readrace.api.model.ItemBiblioteca;
import com.readrace.api.model.Livro;
import com.readrace.api.model.RegistroLeitura;
import com.readrace.api.model.UsuarioId;
import com.readrace.api.repository.ItemBibliotecaRepository;
import com.readrace.api.repository.LivroRepository;
import com.readrace.api.repository.RegistroLeituraRepository;

@Service
public class RegistrarProgressoService {

    private static final int XP_CONCLUSAO = 150;

    private final LivroRepository livroRepository;
    private final ItemBibliotecaRepository itemBibliotecaRepository;
    private final RegistroLeituraRepository registroLeituraRepository;
    private final UsuarioAtualDeSeed usuarioAtual;

    public RegistrarProgressoService(
            LivroRepository livroRepository,
            ItemBibliotecaRepository itemBibliotecaRepository,
            RegistroLeituraRepository registroLeituraRepository,
            UsuarioAtualDeSeed usuarioAtual) {
        this.livroRepository = livroRepository;
        this.itemBibliotecaRepository = itemBibliotecaRepository;
        this.registroLeituraRepository = registroLeituraRepository;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional
    public ProgressoLeituraResponse registrar(UUID livroId, RegistrarProgressoRequest request) {
        int pagina = lerPagina(request);
        Livro livro =
                livroRepository
                        .findById(livroId)
                        .orElseThrow(LivroNaoEncontradoException::new);

        validarPagina(pagina, livro.getTotalPaginas());

        UsuarioId usuarioAtualId = usuarioAtual.idDoUsuarioAtual();

        ItemBiblioteca item =
                itemBibliotecaRepository
                        .findByUsuarioIdAndLivro_Id(usuarioAtualId.valor(), livro.getId())
                        .orElseGet(
                                () ->
                                        itemBibliotecaRepository.save(
                                                new ItemBiblioteca(usuarioAtualId.valor(), livro)));

        int paginaMaximaAnterior = item.getPaginaMaxima();
        boolean concluidoAntes = item.estaConcluido();
        int xpPaginas = Math.max(0, pagina - paginaMaximaAnterior);

        item.registrarProgresso(pagina);
        registroLeituraRepository.save(new RegistroLeitura(item, pagina));

        int xpConclusao = !concluidoAntes && item.estaConcluido() ? XP_CONCLUSAO : 0;
        int percentual = Math.round((pagina * 100f) / livro.getTotalPaginas());

        return new ProgressoLeituraResponse(
                item.getPaginaAtual(),
                item.getPaginaMaxima(),
                livro.getTotalPaginas(),
                percentual,
                xpPaginas,
                xpConclusao,
                xpPaginas + xpConclusao,
                item.estaConcluido());
    }

    private int lerPagina(RegistrarProgressoRequest request) {
        Object pagina = request == null ? null : request.pagina();
        if (pagina instanceof Integer valor) {
            return valor;
        }
        if (pagina instanceof Long valor
                && valor >= Integer.MIN_VALUE
                && valor <= Integer.MAX_VALUE) {
            return valor.intValue();
        }
        if (pagina instanceof String texto && texto.matches("\\d+")) {
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException ex) {
                throw new PaginaInvalidaException(
                        "Informe uma pagina inteira entre 1 e o total do livro.");
            }
        }

        if (pagina instanceof Number) {
            throw new PaginaInvalidaException(
                    "Informe uma pagina inteira entre 1 e o total do livro.");
        }

        throw new PaginaInvalidaException("Informe uma pagina inteira entre 1 e o total do livro.");
    }

    private void validarPagina(int pagina, int totalPaginas) {
        if (pagina <= 0 || pagina > totalPaginas) {
            throw new PaginaInvalidaException(
                    "Informe uma pagina entre 1 e %d.".formatted(totalPaginas));
        }
    }
}
