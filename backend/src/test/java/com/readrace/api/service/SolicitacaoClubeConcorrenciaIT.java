package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.dto.request.PublicarQuizRequest;
import com.readrace.api.dto.request.RegistrarProgressoRequest;
import com.readrace.api.dto.request.ResponderPerguntaRequest;
import com.readrace.api.dto.response.SolicitacaoClubeResponse;
import com.readrace.api.exception.ExcecaoDeNegocio;
import com.readrace.api.model.Usuario;
import com.readrace.api.model.UsuarioId;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SolicitacaoClubeConcorrenciaIT {
    private static final UUID LIVRO = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private final ThreadLocal<UUID> identidade = new ThreadLocal<>();
    private UUID a;
    private UUID b;
    private UUID c;
    private UUID x;
    private UUID y;
    private UUID pedidoX;
    private UUID pedidoY;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private SolicitacaoClubeService service;
    @Autowired private ClubeService clubes;
    @Autowired private RegistrarProgressoService leitura;
    @Autowired private QuizService quiz;
    @Autowired private PublicarQuizService publicarQuiz;
    @MockitoSpyBean private UsuarioAtual usuarioAtual;
    @MockitoSpyBean private ConquistaService conquistas;

    @BeforeEach
    void preparar_dados_confirmados_e_identidade_por_thread() {
        a = novoUsuario();
        b = novoUsuario();
        c = novoUsuario();
        x = novoClube(a);
        y = novoClube(b);
        pedidoX = pedido(x, b);
        pedidoY = pedido(y, a);
        doAnswer(i -> new UsuarioId(identidade.get())).when(usuarioAtual).idDoUsuarioAtual();
    }

    @AfterEach
    void limpar_apenas_dados_proprios() {
        // As transações são confirmadas de verdade; nenhum teste depende de rollback externo.
        for (UUID clube : List.of(x, y)) {
            jdbc.update(
                    "DELETE FROM resposta_quiz WHERE pergunta_id IN (SELECT p.id FROM pergunta_quiz p JOIN quiz q ON q.id = p.quiz_id WHERE q.clube_id = ?)",
                    clube);
            jdbc.update(
                    "DELETE FROM conclusao_quiz WHERE quiz_id IN (SELECT id FROM quiz WHERE clube_id = ?)",
                    clube);
            jdbc.update(
                    "DELETE FROM alternativa_quiz WHERE pergunta_id IN (SELECT p.id FROM pergunta_quiz p JOIN quiz q ON q.id = p.quiz_id WHERE q.clube_id = ?)",
                    clube);
            jdbc.update(
                    "DELETE FROM pergunta_quiz WHERE quiz_id IN (SELECT id FROM quiz WHERE clube_id = ?)",
                    clube);
            jdbc.update("DELETE FROM quiz WHERE clube_id = ?", clube);
            jdbc.update("DELETE FROM solicitacao_entrada_clube WHERE clube_id = ?", clube);
            jdbc.update("DELETE FROM membro_clube WHERE clube_id = ?", clube);
            jdbc.update("DELETE FROM clube_do_livro WHERE id = ?", clube);
        }
        for (UUID usuario : List.of(a, b, c)) {
            jdbc.update(
                    "DELETE FROM registro_leitura WHERE item_biblioteca_id IN (SELECT id FROM item_biblioteca WHERE usuario_id = ?)",
                    usuario);
            jdbc.update("DELETE FROM item_biblioteca WHERE usuario_id = ?", usuario);
            jdbc.update("DELETE FROM conquista_usuario WHERE usuario_id = ?", usuario);
            jdbc.update("DELETE FROM usuario WHERE id = ?", usuario);
        }
    }

    @Test
    void pedidos_simultaneos_devolvem_o_mesmo_id() throws Exception {
        var par =
                serializar(
                        () -> como(c, () -> service.solicitar(x)),
                        () -> como(c, () -> service.solicitar(x)));
        assertThat(par.primeiro.erro).isNull();
        assertThat(par.segundo.erro).isNull();
        assertThat(par.segundo.valor).isEqualTo(par.primeiro.valor);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM solicitacao_entrada_clube WHERE clube_id = ? AND usuario_id = ?",
                                Integer.class,
                                x,
                                c))
                .isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"aceitar", "recusar"})
    void duas_respostas_preservam_a_primeira(String segundaAcao) throws Exception {
        // Dois administradores distintos, não duas chamadas da mesma identidade.
        membro(x, c, "administrador", 0);
        var par =
                serializar(
                        () -> como(a, () -> service.aceitar(x, pedidoX)),
                        () ->
                                como(
                                        c,
                                        () ->
                                                segundaAcao.equals("aceitar")
                                                        ? service.aceitar(x, pedidoX)
                                                        : service.recusar(x, pedidoX)));
        assertThat(((SolicitacaoClubeResponse) par.primeiro.valor).situacao()).isEqualTo("aceita");
        assertThat(par.segundo.erro).isEqualTo("SOLICITACAO_JA_RESPONDIDA");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT administrador_id FROM solicitacao_entrada_clube WHERE id = ?",
                                UUID.class,
                                pedidoX))
                .isEqualTo(a);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM membro_clube WHERE clube_id = ? AND usuario_id = ?",
                                Integer.class,
                                x,
                                b))
                .isEqualTo(1);
    }

    @Test
    void recusa_que_chega_primeiro_prevalece_sobre_aceite() throws Exception {
        var par =
                serializar(
                        () -> como(a, () -> service.recusar(x, pedidoX)),
                        () -> como(a, () -> service.aceitar(x, pedidoX)));
        assertThat(((SolicitacaoClubeResponse) par.primeiro.valor).situacao())
                .isEqualTo("recusada");
        assertThat(par.segundo.erro).isEqualTo("SOLICITACAO_JA_RESPONDIDA");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM membro_clube WHERE clube_id = ? AND usuario_id = ?",
                                Integer.class,
                                x,
                                b))
                .isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void saida_antes_da_resposta_revalida_antigo_ou_novo_lider(boolean novoLider) throws Exception {
        membro(x, c, "membro", 100);
        var par =
                serializar(
                        () -> como(a, () -> clubes.sairDoClube(x)),
                        () -> como(novoLider ? c : a, () -> service.aceitar(x, pedidoX)));
        assertThat(par.primeiro.erro).isNull();
        if (novoLider) {
            assertThat(par.segundo.erro).isNull();
            assertThat(((SolicitacaoClubeResponse) par.segundo.valor).situacao())
                    .isEqualTo("aceita");
        } else {
            assertThat(par.segundo.erro).isEqualTo("SO_LIDER_CRIA_QUIZ");
        }
    }

    @Test
    void resposta_antes_da_saida_e_valida_e_sucessao_usa_novo_membro() throws Exception {
        var par =
                serializar(
                        () -> como(a, () -> service.aceitar(x, pedidoX)),
                        () -> como(a, () -> clubes.sairDoClube(x)));
        assertThat(par.primeiro.erro).isNull();
        assertThat(par.segundo.erro).isNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT cargo_clube::text FROM membro_clube WHERE clube_id = ? AND usuario_id = ?",
                                String.class,
                                x,
                                b))
                .isEqualTo("administrador");
    }

    @Test
    void ultimo_membro_sai_antes_do_pedido() throws Exception {
        var par =
                serializar(
                        () -> como(a, () -> clubes.sairDoClube(x)),
                        () -> como(c, () -> service.solicitar(x)));
        assertThat(par.primeiro.erro).isNull();
        assertThat(par.segundo.erro).isEqualTo("CLUBE_NAO_ENCONTRADO");
    }

    @Test
    void aceites_cruzados_nao_bloqueiam_fks_dos_administradores() throws Exception {
        var ambosComUsuarioTravado = new CountDownLatch(2);
        doAnswer(
                        i -> {
                            ambosComUsuarioTravado.countDown();
                            assertThat(ambosComUsuarioTravado.await(8, TimeUnit.SECONDS)).isTrue();
                            return i.callRealMethod();
                        })
                .when(conquistas)
                .avaliar(any(UUID.class));
        // Cada transação já travou seu solicitante quando chega à avaliação. O responsável
        // de uma resposta é o solicitante da outra: FOR UPDATE nos usuários criaria o ciclo.
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var um =
                    executor.submit(
                            () -> executar(() -> como(a, () -> service.aceitar(x, pedidoX))));
            var dois =
                    executor.submit(
                            () -> executar(() -> como(b, () -> service.aceitar(y, pedidoY))));
            assertThat(um.get(20, TimeUnit.SECONDS).erro).isNull();
            assertThat(dois.get(20, TimeUnit.SECONDS).erro).isNull();
        }
        assertThat(xp(a)).isEqualTo(75);
        assertThat(xp(b)).isEqualTo(75);
    }

    @Test
    void mesmo_solicitante_entra_em_dois_clubes_com_uma_unica_conquista() throws Exception {
        UUID primeiroPedido = pedido(x, c);
        UUID segundoPedido = pedido(y, c);
        var par =
                serializar(
                        () -> como(a, () -> service.aceitar(x, primeiroPedido)),
                        () -> como(b, () -> service.aceitar(y, segundoPedido)));
        assertThat(par.primeiro.erro).isNull();
        assertThat(par.segundo.erro).isNull();
        assertThat(xp(c)).isEqualTo(75);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM membro_clube WHERE usuario_id = ?",
                                Integer.class,
                                c))
                .isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM conquista_usuario WHERE usuario_id = ?",
                                Integer.class,
                                c))
                .isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void aceite_e_avaliacao_de_conquistas_serializam_usuario(boolean aceitePrimeiro)
            throws Exception {
        Supplier<?> aceite = () -> como(a, () -> service.aceitar(x, pedidoX));
        Supplier<?> avaliar =
                () -> {
                    conquistas.avaliar(b);
                    return true;
                };
        var par = aceitePrimeiro ? serializar(aceite, avaliar) : serializar(avaliar, aceite);
        assertThat(par.primeiro.erro).isNull();
        assertThat(par.segundo.erro).isNull();
        assertThat(xp(b)).isEqualTo(75);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM conquista_usuario WHERE usuario_id = ?",
                                Integer.class,
                                b))
                .isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void aceite_e_leitura_preservam_ordem_associacao_usuario(boolean aceitePrimeiro)
            throws Exception {
        membro(x, b, "membro", 123);
        Supplier<?> aceite = () -> como(a, () -> service.aceitar(x, pedidoX));
        Supplier<?> registrar =
                () -> como(b, () -> leitura.registrar(LIVRO, new RegistrarProgressoRequest(1)));
        var par = aceitePrimeiro ? serializar(aceite, registrar) : serializar(registrar, aceite);
        assertThat(par.primeiro.erro).isNull();
        assertThat(par.segundo.erro).isNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT pontos FROM membro_clube WHERE clube_id = ? AND usuario_id = ?",
                                Integer.class,
                                x,
                                b))
                .isEqualTo(124);
        assertThat(xp(b)).isEqualTo(76);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void aceite_e_conclusao_de_quiz_preservam_pontos_e_xp(boolean aceitePrimeiro) throws Exception {
        membro(x, b, "membro", 123);
        executar(
                () ->
                        como(
                                a,
                                () ->
                                        publicarQuiz.publicar(
                                                x,
                                                new PublicarQuizRequest(
                                                        "Quiz",
                                                        20,
                                                        List.of(
                                                                new PublicarQuizRequest
                                                                        .PerguntaRequest(
                                                                        "Pergunta?",
                                                                        List.of("A", "B", "C", "D"),
                                                                        0))))));
        UUID pergunta =
                jdbc.queryForObject(
                        "SELECT p.id FROM pergunta_quiz p JOIN quiz q ON q.id = p.quiz_id WHERE q.clube_id = ?",
                        UUID.class,
                        x);
        UUID alternativa =
                jdbc.queryForObject(
                        "SELECT id FROM alternativa_quiz WHERE pergunta_id = ? AND correta",
                        UUID.class,
                        pergunta);
        Supplier<?> aceite =
                () -> {
                    // Simula um usuário já carregado na sessão antes de esperar pelo lock,
                    // como pode acontecer na resolução de identidade do Cognito.
                    em.find(Usuario.class, b);
                    return como(a, () -> service.aceitar(x, pedidoX));
                };
        Supplier<?> responder =
                () ->
                        como(
                                b,
                                () ->
                                        quiz.responder(
                                                x,
                                                pergunta,
                                                new ResponderPerguntaRequest(alternativa)));
        var par = aceitePrimeiro ? serializar(aceite, responder) : serializar(responder, aceite);
        assertThat(par.primeiro.erro).isNull();
        assertThat(par.segundo.erro).isNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT pontos FROM membro_clube WHERE clube_id = ? AND usuario_id = ?",
                                Integer.class,
                                x,
                                b))
                .isEqualTo(173);
        assertThat(xp(b)).isEqualTo(95);
    }

    /** Mantém a primeira transação aberta até provar que a segunda espera por ela no PostgreSQL. */
    private Par serializar(Supplier<?> primeiro, Supplier<?> segundo) throws Exception {
        var pronto = new CountDownLatch(1);
        var pidSegundo = new AtomicInteger();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var um =
                    executor.submit(
                            () ->
                                    executar(
                                            () -> {
                                                Object valor = primeiro.get();
                                                int pidPrimeiro =
                                                        jdbc.queryForObject(
                                                                "SELECT pg_backend_pid()",
                                                                Integer.class);
                                                pronto.countDown();
                                                long limite =
                                                        System.nanoTime()
                                                                + Duration.ofSeconds(8).toNanos();
                                                while (System.nanoTime() < limite) {
                                                    if (pidSegundo.get() != 0
                                                            && Boolean.TRUE.equals(
                                                                    jdbc.queryForObject(
                                                                            "SELECT ? = ANY(pg_blocking_pids(?))",
                                                                            Boolean.class,
                                                                            pidPrimeiro,
                                                                            pidSegundo.get()))) {
                                                        return valor;
                                                    }
                                                    LockSupport.parkNanos(
                                                            TimeUnit.MILLISECONDS.toNanos(10));
                                                }
                                                throw new AssertionError(
                                                        "A segunda transação não esperou o lock da primeira");
                                            }));
            assertThat(pronto.await(10, TimeUnit.SECONDS)).isTrue();
            var dois =
                    executor.submit(
                            () ->
                                    executar(
                                            () -> {
                                                pidSegundo.set(
                                                        jdbc.queryForObject(
                                                                "SELECT pg_backend_pid()",
                                                                Integer.class));
                                                return segundo.get();
                                            }));
            return new Par(um.get(20, TimeUnit.SECONDS), dois.get(20, TimeUnit.SECONDS));
        }
    }

    private Resultado executar(Supplier<?> operacao) {
        try {
            Object valor =
                    new TransactionTemplate(transactionManager)
                            .execute(
                                    status -> {
                                        jdbc.execute("SET LOCAL lock_timeout = '10s'");
                                        jdbc.execute("SET LOCAL statement_timeout = '15s'");
                                        return operacao.get();
                                    });
            return new Resultado(valor, null);
        } catch (ExcecaoDeNegocio erro) {
            return new Resultado(null, erro.getCodigo().name());
        }
    }

    private <T> T como(UUID usuario, Supplier<T> operacao) {
        identidade.set(usuario);
        try {
            return operacao.get();
        } finally {
            identidade.remove();
        }
    }

    private UUID novoUsuario() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO usuario (id, nome, nome_usuario, email) VALUES (?, 'Concorrência', ?, ?)",
                id,
                id.toString(),
                id + "@example.com");
        return id;
    }

    private UUID novoClube(UUID administrador) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO clube_do_livro (id, nome, livro_id) VALUES (?, 'Concorrência', ?)",
                id,
                LIVRO);
        membro(id, administrador, "administrador", 0);
        return id;
    }

    private void membro(UUID clube, UUID usuario, String cargo, int pontos) {
        jdbc.update(
                "INSERT INTO membro_clube (id, clube_id, usuario_id, cargo_clube, pontos) VALUES (?, ?, ?, ?::cargo, ?)",
                UUID.randomUUID(),
                clube,
                usuario,
                cargo,
                pontos);
    }

    private UUID pedido(UUID clube, UUID usuario) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO solicitacao_entrada_clube (id, clube_id, usuario_id, situacao, criada_em) VALUES (?, ?, ?, 'pendente', now())",
                id,
                clube,
                usuario);
        return id;
    }

    private int xp(UUID usuario) {
        return jdbc.queryForObject(
                "SELECT xp_total FROM usuario WHERE id = ?", Integer.class, usuario);
    }

    private record Resultado(Object valor, String erro) {}

    private record Par(Resultado primeiro, Resultado segundo) {}
}
