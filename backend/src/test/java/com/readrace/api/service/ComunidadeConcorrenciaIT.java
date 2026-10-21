package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.exception.UnicoAdministradorException;
import com.readrace.api.model.UsuarioId;
import com.readrace.api.repository.ComunidadeRepository;
import com.readrace.api.repository.CurtidaRepository;
import com.readrace.api.repository.MembroComunidadeRepository;
import com.readrace.api.repository.PostRepository;
import com.readrace.api.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ComunidadeConcorrenciaIT {
    @Autowired private ComunidadeRepository comunidades;
    @Autowired private MembroComunidadeRepository membros;
    @Autowired private PostRepository posts;
    @Autowired private CurtidaRepository curtidas;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    private ComunidadeService como(UUID usuario) {
        // Cada chamada representa um usuário; as transações reais são abertas abaixo.
        return new ComunidadeService(
                comunidades, membros, posts, curtidas, usuarios, () -> new UsuarioId(usuario));
    }

    @Test
    void doisAdministradoresNaoPodemSairAoMesmoTempo() throws Exception {
        UUID comunidade = UUID.randomUUID();
        UUID primeiro = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID segundo = UUID.fromString("00000000-0000-0000-0000-000000000002");
        jdbc.update("INSERT INTO comunidade (id, nome) VALUES (?, 'Concorrência')", comunidade);
        for (UUID usuario : new UUID[] {primeiro, segundo}) {
            jdbc.update(
                    "INSERT INTO membro_comunidade (id, comunidade_id, usuario_id, cargo_comunidade) VALUES (?, ?, ?, 'administrador')",
                    UUID.randomUUID(),
                    comunidade,
                    usuario);
        }
        var transacao = new TransactionTemplate(transactionManager);
        var primeiraSaida = new CountDownLatch(1);
        var pidSegunda = new AtomicInteger();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var chamada1 =
                    executor.submit(
                            () ->
                                    transacao.execute(
                                            status -> {
                                                var resposta =
                                                        como(primeiro).deixarDeSeguir(comunidade);
                                                primeiraSaida.countDown();
                                                // Mantém a exclusão sem commit até comprovar que a
                                                // segunda saída espera.
                                                for (int i = 0; i < 200; i++) {
                                                    jdbc.execute("SELECT pg_stat_clear_snapshot()");
                                                    Boolean bloqueada =
                                                            jdbc.queryForObject(
                                                                    "SELECT EXISTS (SELECT 1 FROM pg_stat_activity WHERE pid = ? AND wait_event_type = 'Lock')",
                                                                    Boolean.class,
                                                                    pidSegunda.get());
                                                    if (Boolean.TRUE.equals(bloqueada))
                                                        return resposta;
                                                    LockSupport.parkNanos(
                                                            TimeUnit.MILLISECONDS.toNanos(50));
                                                }
                                                throw new AssertionError(
                                                        "A segunda saída não aguardou o lock da comunidade");
                                            }));
            assertThat(primeiraSaida.await(15, TimeUnit.SECONDS)).isTrue();
            var chamada2 =
                    executor.submit(
                            () -> {
                                assertThatThrownBy(
                                                () ->
                                                        transacao.execute(
                                                                status -> {
                                                                    pidSegunda.set(
                                                                            jdbc.queryForObject(
                                                                                    "SELECT pg_backend_pid()",
                                                                                    Integer.class));
                                                                    return como(segundo)
                                                                            .deixarDeSeguir(
                                                                                    comunidade);
                                                                }))
                                        .isInstanceOf(UnicoAdministradorException.class)
                                        .hasMessage(
                                                "Você é o único administrador desta comunidade e não pode sair.");
                            });
            assertThat(chamada1.get(30, TimeUnit.SECONDS).sigo()).isFalse();
            chamada2.get(30, TimeUnit.SECONDS);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM membro_comunidade WHERE comunidade_id = ? AND cargo_comunidade = 'administrador'",
                                    Integer.class,
                                    comunidade))
                    .isEqualTo(1);
        } finally {
            jdbc.update("DELETE FROM membro_comunidade WHERE comunidade_id = ?", comunidade);
            jdbc.update("DELETE FROM comunidade WHERE id = ?", comunidade);
        }
    }
}
