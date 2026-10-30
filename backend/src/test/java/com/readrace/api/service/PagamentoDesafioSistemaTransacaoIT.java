package com.readrace.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.dto.request.RegistrarProgressoRequest;
import com.readrace.api.dto.response.ProgressoLeituraResponse;
import com.readrace.api.model.CurvaDeNivel;
import com.readrace.api.model.UsuarioId;
import com.readrace.api.repository.UsuarioRepository;

@SpringBootTest
@Import({TestcontainersConfiguration.class, PagamentoDesafioSistemaTransacaoIT.Config.class})
class PagamentoDesafioSistemaTransacaoIT {
    private static final UUID USUARIO = UUID.fromString("20540000-0000-0000-0000-000000000001");
    private static final Instant AGORA = Instant.parse("2026-10-24T15:00:00Z");
    private static final UUID CLUBE = UUID.fromString("20540000-0000-0000-0000-000000000002");
    @Autowired JdbcTemplate jdbc;
    @Autowired RegistrarProgressoService leitura;
    @Autowired DesafiosSistemaService desafios;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired UsuarioComPreCarga usuarioAtual;
    @MockitoBean Clock clock;

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        UsuarioComPreCarga usuarioDoTeste(UsuarioRepository usuarios) {
            return new UsuarioComPreCarga(usuarios);
        }
    }

    static class UsuarioComPreCarga implements UsuarioAtual {
        private final UsuarioRepository usuarios;
        volatile CyclicBarrier preCarregados;

        UsuarioComPreCarga(UsuarioRepository usuarios) {
            this.usuarios = usuarios;
        }

        @Override
        public UsuarioId idDoUsuarioAtual() {
            // Reproduz o carregamento gerenciado feito pelo resolvedor de token Cognito.
            usuarios.findByIdAndExcluidoEmIsNull(USUARIO).orElseThrow();
            var barreira = preCarregados;
            if (barreira != null) {
                try {
                    barreira.await(10, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    throw new IllegalStateException("Falha na sincronização da pré-carga", ex);
                }
            }
            return new UsuarioId(USUARIO);
        }
    }

    @BeforeEach
    void preparar() {
        when(clock.instant()).thenReturn(AGORA);
        usuarioAtual.preCarregados = null;
        jdbc.update(
                "INSERT INTO usuario (id, nome, nome_usuario, email) VALUES (?, 'Teste transação 205', 'teste205transacao', 'teste205transacao@example.com')",
                USUARIO);
        jdbc.update(
                "INSERT INTO conquista_usuario (id, conquista_id, usuario_id) SELECT gen_random_uuid(), id, ? FROM conquista",
                USUARIO);
    }

    @AfterEach
    void limpar_fixtures_confirmadas() {
        usuarioAtual.preCarregados = null;
        jdbc.execute(
                "DROP TRIGGER IF EXISTS teste205_falha_pagamento ON pagamento_desafio_sistema");
        jdbc.execute("DROP FUNCTION IF EXISTS teste205_falha_pagamento()");
        jdbc.update("DELETE FROM pagamento_desafio_sistema WHERE usuario_id = ?", USUARIO);
        jdbc.update(
                "DELETE FROM registro_leitura WHERE item_biblioteca_id IN (SELECT id FROM item_biblioteca WHERE usuario_id = ?)",
                USUARIO);
        jdbc.update("DELETE FROM item_biblioteca WHERE usuario_id = ?", USUARIO);
        jdbc.update("DELETE FROM conquista_usuario WHERE usuario_id = ?", USUARIO);
        jdbc.update("DELETE FROM membro_clube WHERE clube_id = ?", CLUBE);
        jdbc.update("DELETE FROM clube_do_livro WHERE id = ?", CLUBE);
        jdbc.update("DELETE FROM usuario WHERE id = ?", USUARIO);
    }

    @Test
    void livros_diferentes_com_usuario_pre_carregado_preservam_xp_e_um_pagamento()
            throws Exception {
        item(1, 30);
        item(2, 30);
        usuarioAtual.preCarregados = new CyclicBarrier(2);
        var respostas = concorrer(1, 2, 40);
        assertThat(respostas).hasSize(2);
        assertThat(xp()).isEqualTo(120);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT nivel FROM usuario WHERE id = ?", Integer.class, USUARIO))
                .isEqualTo(CurvaDeNivel.nivelDoXp(120));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM pagamento_desafio_sistema WHERE usuario_id = ?",
                                Long.class,
                                USUARIO))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT SUM(pagina_maxima) FROM item_biblioteca WHERE usuario_id = ?",
                                Integer.class,
                                USUARIO))
                .isEqualTo(80);
        assertThat(respostas.stream().mapToInt(ProgressoLeituraResponse::xpTotal).sum())
                .isEqualTo(20);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT SUM(r.paginas_novas) FROM registro_leitura r JOIN item_biblioteca i ON i.id = r.item_biblioteca_id WHERE i.usuario_id = ?",
                                Integer.class,
                                USUARIO))
                .isEqualTo(80);
    }

    @Test
    void mesmo_livro_concorrente_paga_cada_pagina_e_desafio_uma_vez() throws Exception {
        item(1, 60);
        usuarioAtual.preCarregados = new CyclicBarrier(2);
        var respostas = concorrer(1, 1, 75);
        assertThat(respostas.stream().mapToInt(ProgressoLeituraResponse::xpTotal).sum())
                .isEqualTo(15);
        assertThat(xp()).isEqualTo(115);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM pagamento_desafio_sistema WHERE usuario_id = ?",
                                Long.class,
                                USUARIO))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT pagina_maxima FROM item_biblioteca WHERE usuario_id = ?",
                                Integer.class,
                                USUARIO))
                .isEqualTo(75);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT SUM(r.paginas_novas) FROM registro_leitura r JOIN item_biblioteca i ON i.id = r.item_biblioteca_id WHERE i.usuario_id = ?",
                                Integer.class,
                                USUARIO))
                .isEqualTo(75);
    }

    @Test
    void conclusoes_concorrentes_pagaram_ambos_desafios_uma_vez() throws Exception {
        Instant semanaAnterior = Instant.parse("2026-10-16T15:00:00Z");
        UUID primeiro = item(1, total(1));
        jdbc.update(
                "UPDATE registro_leitura SET registrado_em = ? WHERE item_biblioteca_id = ?",
                semanaAnterior.atOffset(ZoneOffset.UTC),
                primeiro);
        for (int numero : new int[] {2, 3}) {
            UUID item = item(numero, total(numero) - 15);
            jdbc.update(
                    "UPDATE registro_leitura SET ultima_pagina = ?, paginas_novas = ?, registrado_em = ? WHERE item_biblioteca_id = ?",
                    total(numero) - 45,
                    total(numero) - 45,
                    semanaAnterior.atOffset(ZoneOffset.UTC),
                    item);
            jdbc.update(
                    "INSERT INTO registro_leitura (id, item_biblioteca_id, ultima_pagina, paginas_novas, registrado_em) VALUES (?, ?, ?, 30, ?)",
                    UUID.randomUUID(),
                    item,
                    total(numero) - 15,
                    AGORA.minusSeconds(3600).atOffset(ZoneOffset.UTC));
        }
        usuarioAtual.preCarregados = new CyclicBarrier(2);
        var respostas = concorrer(2, 3, -1);
        assertThat(respostas.stream().mapToInt(ProgressoLeituraResponse::xpTotal).sum())
                .isEqualTo(330);
        assertThat(xp()).isEqualTo(630);
        assertThat(
                        jdbc.queryForList(
                                "SELECT xp_pago FROM pagamento_desafio_sistema WHERE usuario_id = ? ORDER BY xp_pago",
                                Integer.class,
                                USUARIO))
                .containsExactly(100, 200);
        assertThat(desafios.medir(USUARIO, AGORA.atOffset(ZoneOffset.UTC).toLocalDate(), AGORA))
                .isEqualTo(new DesafiosSistemaService.Medicao(3, 90));
    }

    @Test
    void falha_no_segundo_pagamento_reverte_primeiro_leitura_xp_nivel_e_pontos() {
        Instant anterior = Instant.parse("2026-10-16T15:00:00Z");
        UUID primeiro = item(1, total(1));
        UUID segundo = item(2, total(2) - 15);
        jdbc.update(
                "UPDATE registro_leitura SET registrado_em = ? WHERE item_biblioteca_id = ?",
                anterior.atOffset(ZoneOffset.UTC),
                primeiro);
        jdbc.update(
                "UPDATE registro_leitura SET ultima_pagina = ?, paginas_novas = ?, registrado_em = ? WHERE item_biblioteca_id = ?",
                total(2) - 75,
                total(2) - 75,
                anterior.atOffset(ZoneOffset.UTC),
                segundo);
        jdbc.update(
                "INSERT INTO registro_leitura (id, item_biblioteca_id, ultima_pagina, paginas_novas, registrado_em) VALUES (?, ?, ?, 60, ?)",
                UUID.randomUUID(),
                segundo,
                total(2) - 15,
                AGORA.minusSeconds(3600).atOffset(ZoneOffset.UTC));
        jdbc.update(
                "INSERT INTO clube_do_livro (id, livro_id, nome) VALUES (?, ?, 'Clube rollback 205')",
                CLUBE,
                livro(2));
        jdbc.update(
                "INSERT INTO membro_clube (id, clube_id, usuario_id, pontos, cargo_clube) VALUES (?, ?, ?, 7, 'membro')",
                UUID.randomUUID(),
                CLUBE,
                USUARIO);
        var antes = estado();
        // A falha ocorre no INSERT real do segundo pagamento e exige que o primeiro já exista
        // dentro da mesma transação. O trigger só existe no banco de teste e só afeta esta fixture.
        jdbc.execute(
                """
                CREATE FUNCTION teste205_falha_pagamento() RETURNS TRIGGER AS $$
                BEGIN
                    IF NEW.usuario_id = '20540000-0000-0000-0000-000000000001'::uuid
                       AND NEW.desafio = 'SEMANAL' THEN
                        IF NOT EXISTS (SELECT 1 FROM pagamento_desafio_sistema
                                       WHERE usuario_id = NEW.usuario_id AND desafio = 'MENSAL') THEN
                            RAISE EXCEPTION 'Teste 205 não chegou ao primeiro pagamento';
                        END IF;
                        RAISE EXCEPTION 'Falha teste 205 após pagamento mensal';
                    END IF;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """);
        jdbc.execute(
                "CREATE TRIGGER teste205_falha_pagamento BEFORE INSERT ON pagamento_desafio_sistema FOR EACH ROW EXECUTE FUNCTION teste205_falha_pagamento()");

        assertThatThrownBy(
                        () -> leitura.registrar(livro(2), new RegistrarProgressoRequest(total(2))))
                .hasStackTraceContaining("Falha teste 205 após pagamento mensal");
        // A classe não tem @Transactional: o proxy já encerrou e desfez a transação do serviço.
        // Estas consultas usam novas transações/conexões e comparam todos os efeitos exercitados.
        assertThat(estado()).isEqualTo(antes);
        assertThat(xp()).isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM pagamento_desafio_sistema WHERE usuario_id = ?",
                                Long.class,
                                USUARIO))
                .isZero();
    }

    private Map<String, List<Map<String, Object>>> estado() {
        return Map.of(
                "usuario", jdbc.queryForList("SELECT * FROM usuario WHERE id = ?", USUARIO),
                "itens",
                        jdbc.queryForList(
                                "SELECT * FROM item_biblioteca WHERE usuario_id = ? ORDER BY id",
                                USUARIO),
                "leituras",
                        jdbc.queryForList(
                                "SELECT r.* FROM registro_leitura r JOIN item_biblioteca i ON i.id = r.item_biblioteca_id WHERE i.usuario_id = ? ORDER BY r.id",
                                USUARIO),
                "pagamentos",
                        jdbc.queryForList(
                                "SELECT * FROM pagamento_desafio_sistema WHERE usuario_id = ? ORDER BY id",
                                USUARIO),
                "pontos",
                        jdbc.queryForList(
                                "SELECT * FROM membro_clube WHERE usuario_id = ? ORDER BY id",
                                USUARIO),
                "conquistas",
                        jdbc.queryForList(
                                "SELECT * FROM conquista_usuario WHERE usuario_id = ? ORDER BY id",
                                USUARIO));
    }

    private int total(int numero) {
        return jdbc.queryForObject(
                "SELECT total_paginas FROM livro WHERE id = ?", Integer.class, livro(numero));
    }

    private List<ProgressoLeituraResponse> concorrer(int primeiro, int segundo, int pagina)
            throws Exception {
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        List<Future<ProgressoLeituraResponse>> tarefas = new ArrayList<>();
        List<ProgressoLeituraResponse> respostas = new ArrayList<>();
        List<Throwable> falhas = new ArrayList<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int numero : new int[] {primeiro, segundo}) {
                tarefas.add(
                        executor.submit(
                                () -> {
                                    prontas.countDown();
                                    if (!inicio.await(10, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException("Início não liberado");
                                    }
                                    return new TransactionTemplate(transactionManager)
                                            .execute(
                                                    status -> {
                                                        jdbc.execute(
                                                                "SET LOCAL lock_timeout = '5s'");
                                                        jdbc.execute(
                                                                "SET LOCAL statement_timeout = '10s'");
                                                        return leitura.registrar(
                                                                livro(numero),
                                                                new RegistrarProgressoRequest(
                                                                        pagina < 0
                                                                                ? total(numero)
                                                                                : pagina));
                                                    });
                                }));
            }
            try {
                assertThat(prontas.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                inicio.countDown();
            }
            for (var tarefa : tarefas) {
                try {
                    respostas.add(tarefa.get(20, TimeUnit.SECONDS));
                } catch (Exception ex) {
                    falhas.add(ex);
                    tarefa.cancel(true);
                }
            }
        }
        assertThat(falhas).as("Falhas das transações concorrentes").isEmpty();
        return respostas;
    }

    private UUID livro(int numero) {
        return UUID.fromString("30000000-0000-0000-0000-%012d".formatted(numero));
    }

    private UUID item(int numero, int maxima) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO item_biblioteca (id, usuario_id, livro_id, status_leitura, pagina_atual, pagina_maxima) VALUES (?, ?, ?, 'lendo', ?, ?)",
                id,
                USUARIO,
                livro(numero),
                maxima,
                maxima);
        jdbc.update(
                "INSERT INTO registro_leitura (id, item_biblioteca_id, ultima_pagina, paginas_novas, registrado_em) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                id,
                maxima,
                maxima,
                AGORA.minusSeconds(3600).atOffset(ZoneOffset.UTC));
        return id;
    }

    private int xp() {
        return jdbc.queryForObject(
                "SELECT xp_total FROM usuario WHERE id = ?", Integer.class, USUARIO);
    }
}
