package com.readrace.api.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.request.RegistrarProgressoRequest;
import com.readrace.api.dto.response.ProgressoLeituraResponse;
import com.readrace.api.exception.LivroNaoEncontradoException;
import com.readrace.api.exception.PaginaInvalidaException;
import com.readrace.api.exception.UsuarioNaoEncontradoException;
import com.readrace.api.model.CurvaDeNivel;
import com.readrace.api.model.ItemBiblioteca;
import com.readrace.api.model.Livro;
import com.readrace.api.model.MembroClube;
import com.readrace.api.model.RegistroLeitura;
import com.readrace.api.model.SequenciaDeLeitura;
import com.readrace.api.model.Usuario;
import com.readrace.api.model.UsuarioId;
import com.readrace.api.repository.ItemBibliotecaRepository;
import com.readrace.api.repository.LivroRepository;
import com.readrace.api.repository.MembroClubeRepository;
import com.readrace.api.repository.RegistroLeituraRepository;
import com.readrace.api.repository.UsuarioRepository;

@Service
public class RegistrarProgressoService {

    private static final int XP_CONCLUSAO = 150;
    private static final int PONTOS_POR_PAGINA = 1;

    private final LivroRepository livroRepository;
    private final ItemBibliotecaRepository itemBibliotecaRepository;
    private final RegistroLeituraRepository registroLeituraRepository;
    private final UsuarioRepository usuarioRepository;
    private final MembroClubeRepository membroClubeRepository;
    private final UsuarioAtual usuarioAtual;
    private final DesafioService desafioService;
    private final ConquistaService conquistaService;

    public RegistrarProgressoService(
            LivroRepository livroRepository,
            ItemBibliotecaRepository itemBibliotecaRepository,
            RegistroLeituraRepository registroLeituraRepository,
            UsuarioRepository usuarioRepository,
            MembroClubeRepository membroClubeRepository,
            UsuarioAtual usuarioAtual,
            DesafioService desafioService,
            ConquistaService conquistaService) {
        this.livroRepository = livroRepository;
        this.itemBibliotecaRepository = itemBibliotecaRepository;
        this.registroLeituraRepository = registroLeituraRepository;
        this.usuarioRepository = usuarioRepository;
        this.membroClubeRepository = membroClubeRepository;
        this.usuarioAtual = usuarioAtual;
        this.desafioService = desafioService;
        this.conquistaService = conquistaService;
    }

    @Transactional
    public ProgressoLeituraResponse registrar(UUID livroId, RegistrarProgressoRequest request) {
        int pagina = lerPagina(request);
        Livro livro =
                livroRepository.findById(livroId).orElseThrow(LivroNaoEncontradoException::new);

        validarPagina(pagina, livro.getTotalPaginas());

        UsuarioId usuarioAtualId = usuarioAtual.idDoUsuarioAtual();

        // A restrição única também serializa a criação quando o item ainda não existe.
        itemBibliotecaRepository.criarSeAusente(UUID.randomUUID(), usuarioAtualId.valor(), livroId);
        ItemBiblioteca item =
                itemBibliotecaRepository
                        .findByUsuarioIdAndLivro_Id(usuarioAtualId.valor(), livro.getId())
                        .orElseThrow();

        int paginaMaximaAnterior = item.getPaginaMaxima();
        boolean concluidoAntes = paginaMaximaAnterior >= livro.getTotalPaginas();
        int paginasNovas = Math.max(0, pagina - paginaMaximaAnterior);
        int xpPaginas = paginasNovas;

        item.registrarProgresso(pagina);
        registroLeituraRepository.save(new RegistroLeitura(item, pagina));

        // Antes do lock do usuário, na mesma ordem do quiz (membro do clube, depois usuário): se
        // cada fluxo pegasse os dois locks em uma ordem, um ficaria esperando o outro (deadlock).
        somarPontosNosClubes(usuarioAtualId.valor(), livro.getId(), paginasNovas);

        // Um único lock pessimista no usuário cobre a sequência de leitura e a soma de XP.
        Usuario usuario =
                usuarioRepository
                        .buscarAtivoComLock(usuarioAtualId.valor())
                        .orElseThrow(UsuarioNaoEncontradoException::new);
        usuario.registrarLeitura(SequenciaDeLeitura.hoje());

        boolean concluiuAgora = !concluidoAntes && item.estaConcluido();
        int xpConclusao = concluiuAgora ? XP_CONCLUSAO : 0;
        int xpGanho = xpPaginas + xpConclusao;
        int percentual = Math.round((pagina * 100f) / livro.getTotalPaginas());

        // Desafio e conquista também pagam XP no mesmo usuário: o nível de antes é guardado aqui e
        // os campos de nível só são calculados depois de todas as recompensas.
        int nivelAnterior = usuario.getNivel();
        desafioService.avancarDesafios(usuarioAtualId.valor(), livro, pagina, paginasNovas);

        usuario.receberXp(xpGanho);
        // Depois da soma de XP, para a avaliação já enxergar o XP e o nível atualizados.
        conquistaService.avaliar(usuarioAtualId.valor());

        boolean subiuDeNivel = usuario.getNivel() != nivelAnterior;
        int xpNoNivel = CurvaDeNivel.xpNoNivel(usuario.getXpTotal(), usuario.getNivel());
        int xpDoNivel = CurvaDeNivel.xpDoNivel(usuario.getNivel());

        return new ProgressoLeituraResponse(
                item.getPaginaAtual(),
                item.getPaginaMaxima(),
                livro.getTotalPaginas(),
                percentual,
                xpPaginas,
                xpConclusao,
                xpGanho,
                item.estaConcluido(),
                usuario.getDiasConsecutivos(),
                usuario.getNivel(),
                usuario.getXpTotal(),
                xpNoNivel,
                xpDoNivel,
                subiuDeNivel);
    }

    /**
     * A leitura é a base da corrida do clube (#163): cada página nova vale {@value
     * #PONTOS_POR_PAGINA} ponto em todos os clubes do usuário cujo livro atual é o livro
     * registrado, seja ele administrador ou membro.
     *
     * <p>{@code Pontos} e {@code XP} são moedas separadas: os dois saem das mesmas páginas novas,
     * cada um na sua conta, e o bônus de conclusão continua sendo só {@code XP}.
     */
    private void somarPontosNosClubes(UUID usuarioId, UUID livroId, int paginasNovas) {
        // Voltar ou reler não paga, e sem página nova nem vale a pena pegar os locks.
        if (paginasNovas == 0) {
            return;
        }

        int pontosGanhos = paginasNovas * PONTOS_POR_PAGINA;
        for (MembroClube membro : membroClubeRepository.buscarDoLivroComLock(usuarioId, livroId)) {
            membro.somarPontos(pontosGanhos);
        }
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
                        "Informe uma página inteira entre 1 e o total do livro.");
            }
        }

        if (pagina instanceof Number) {
            throw new PaginaInvalidaException(
                    "Informe uma página inteira entre 1 e o total do livro.");
        }

        throw new PaginaInvalidaException("Informe uma página inteira entre 1 e o total do livro.");
    }

    private void validarPagina(int pagina, int totalPaginas) {
        if (pagina <= 0 || pagina > totalPaginas) {
            throw new PaginaInvalidaException(
                    "Informe uma página entre 1 e %d.".formatted(totalPaginas));
        }
    }
}
