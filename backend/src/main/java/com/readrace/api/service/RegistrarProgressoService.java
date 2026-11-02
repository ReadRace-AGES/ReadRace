package com.readrace.api.service;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.request.RegistrarProgressoRequest;
import com.readrace.api.dto.response.ProgressoLeituraResponse;
import com.readrace.api.exception.LivroNaoEncontradoException;
import com.readrace.api.exception.PaginaInvalidaException;
import com.readrace.api.exception.UsuarioNaoEncontradoException;
import com.readrace.api.model.CurvaDeNivel;
import com.readrace.api.model.DesafioSistema;
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
    private final DesafiosSistemaService desafiosSistemaService;
    private final PagamentoDesafioSistemaService pagamentoDesafioSistemaService;
    private final Clock clock;
    private final EntityManager entityManager;

    public RegistrarProgressoService(
            LivroRepository livroRepository,
            ItemBibliotecaRepository itemBibliotecaRepository,
            RegistroLeituraRepository registroLeituraRepository,
            UsuarioRepository usuarioRepository,
            MembroClubeRepository membroClubeRepository,
            UsuarioAtual usuarioAtual,
            DesafioService desafioService,
            ConquistaService conquistaService,
            DesafiosSistemaService desafiosSistemaService,
            PagamentoDesafioSistemaService pagamentoDesafioSistemaService,
            Clock clock,
            EntityManager entityManager) {
        this.livroRepository = livroRepository;
        this.itemBibliotecaRepository = itemBibliotecaRepository;
        this.registroLeituraRepository = registroLeituraRepository;
        this.usuarioRepository = usuarioRepository;
        this.membroClubeRepository = membroClubeRepository;
        this.usuarioAtual = usuarioAtual;
        this.desafioService = desafioService;
        this.conquistaService = conquistaService;
        this.desafiosSistemaService = desafiosSistemaService;
        this.pagamentoDesafioSistemaService = pagamentoDesafioSistemaService;
        this.clock = clock;
        this.entityManager = entityManager;
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

        // Antes do lock do usuário, na mesma ordem do quiz (membro do clube, depois usuário): se
        // cada fluxo pegasse os dois locks em uma ordem, um ficaria esperando o outro (deadlock).
        somarPontosNosClubes(usuarioAtualId.valor(), livro.getId(), paginasNovas);

        // Um único lock pessimista no usuário cobre a sequência de leitura e a soma de XP.
        Usuario usuario =
                usuarioRepository
                        .buscarAtivoComLock(usuarioAtualId.valor())
                        .orElseThrow(UsuarioNaoEncontradoException::new);
        // O resolvedor Cognito pode ter carregado o usuário antes do lock. A consulta com lock
        // não atualiza uma entidade já gerenciada: recarregamos o XP confirmado por quem
        // esperávamos.
        entityManager.refresh(usuario);
        int nivelAnterior = usuario.getNivel();
        Instant instante = clock.instant();
        OffsetDateTime registradoEm = instante.atZone(DesafioSistema.FUSO).toOffsetDateTime();
        var dataDoPeriodo = registradoEm.toLocalDate();
        var anterior = desafiosSistemaService.medir(usuario.getId(), dataDoPeriodo, instante);

        // Nenhum registro atual pode estar pendente antes da medição anterior, pois consultas
        // e locks podem provocar flush automático. Só criamos a leitura depois do lock do usuário.
        item.registrarProgresso(pagina);
        registroLeituraRepository.save(
                new RegistroLeitura(item, pagina, paginasNovas, registradoEm));
        registroLeituraRepository.flush();
        var posterior = desafiosSistemaService.medir(usuario.getId(), dataDoPeriodo, instante);
        pagamentoDesafioSistemaService.pagarCruzamentos(
                usuario, dataDoPeriodo, registradoEm, anterior, posterior);

        usuario.registrarLeitura(SequenciaDeLeitura.hoje());

        boolean concluiuAgora = !concluidoAntes && item.estaConcluido();
        int xpConclusao = concluiuAgora ? XP_CONCLUSAO : 0;
        int xpGanho = xpPaginas + xpConclusao;
        int percentual = Math.round((pagina * 100f) / livro.getTotalPaginas());

        // Os campos de nível só são calculados depois de todas as recompensas.
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
