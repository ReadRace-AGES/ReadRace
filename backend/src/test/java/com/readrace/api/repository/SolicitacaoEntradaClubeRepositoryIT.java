package com.readrace.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.readrace.api.TestcontainersConfiguration;
import com.readrace.api.model.SituacaoSolicitacaoClube;
import com.readrace.api.model.SolicitacaoEntradaClube;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class SolicitacaoEntradaClubeRepositoryIT {
    private static final UUID CLUBE = UUID.fromString("50000000-0000-0000-0000-000000000004");
    private static final UUID OUTRO_CLUBE = UUID.fromString("50000000-0000-0000-0000-000000000005");
    private static final UUID USUARIO = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OUTRO_USUARIO =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-10-30T12:00:00Z");

    @Autowired private SolicitacaoEntradaClubeRepository repository;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    @ParameterizedTest
    @EnumSource(SituacaoSolicitacaoClube.class)
    void insere_e_rele_enum_no_postgresql(SituacaoSolicitacaoClube situacao) {
        var solicitacao = new SolicitacaoEntradaClube(CLUBE, USUARIO, AGORA);
        if (situacao != SituacaoSolicitacaoClube.PENDENTE) {
            solicitacao.responder(situacao, AGORA.plusHours(1), OUTRO_USUARIO);
        }
        repository.saveAndFlush(solicitacao);
        UUID id = solicitacao.getId();
        em.clear();

        var lida = repository.findById(id).orElseThrow();
        assertThat(lida.getSituacao()).isEqualTo(situacao);
        assertThat(lida.getClubeId()).isEqualTo(CLUBE);
        assertThat(lida.getUsuarioId()).isEqualTo(USUARIO);
        assertThat(lida.getCriadaEm()).isEqualTo(AGORA);
        assertThat(lida.getRespondidaEm()).isEqualTo(solicitacao.getRespondidaEm());
        assertThat(lida.getAdministradorId()).isEqualTo(solicitacao.getAdministradorId());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT situacao::text FROM solicitacao_entrada_clube WHERE id = ?",
                                String.class,
                                id))
                .isEqualTo(situacao.getValor());
    }

    @Test
    void recusa_libera_novo_pedido_e_preserva_historico() {
        var anterior = repository.saveAndFlush(new SolicitacaoEntradaClube(CLUBE, USUARIO, AGORA));
        anterior.responder(SituacaoSolicitacaoClube.RECUSADA, AGORA.plusHours(1), OUTRO_USUARIO);
        repository.flush();
        var novo =
                repository.saveAndFlush(
                        new SolicitacaoEntradaClube(CLUBE, USUARIO, AGORA.plusDays(1)));
        em.clear();
        assertThat(repository.buscarPendente(CLUBE, USUARIO))
                .get()
                .extracting(SolicitacaoEntradaClube::getId)
                .isEqualTo(novo.getId());
        assertThat(repository.findById(anterior.getId()))
                .get()
                .extracting(SolicitacaoEntradaClube::getSituacao)
                .isEqualTo(SituacaoSolicitacaoClube.RECUSADA);
        assertThat(repository.contarPendentes(CLUBE)).isEqualTo(1);
    }

    @Test
    void filtra_ordena_desempata_conta_e_respeita_clube() {
        var maisNovo =
                repository.saveAndFlush(
                        new SolicitacaoEntradaClube(CLUBE, USUARIO, AGORA.plusDays(1)));
        var primeiro =
                repository.saveAndFlush(new SolicitacaoEntradaClube(CLUBE, OUTRO_USUARIO, AGORA));
        var segundo =
                repository.saveAndFlush(
                        new SolicitacaoEntradaClube(
                                CLUBE,
                                UUID.fromString("00000000-0000-0000-0000-000000000003"),
                                AGORA));
        var respondido =
                repository.saveAndFlush(
                        new SolicitacaoEntradaClube(
                                CLUBE,
                                UUID.fromString("00000000-0000-0000-0000-000000000004"),
                                AGORA.minusDays(1)));
        respondido.responder(SituacaoSolicitacaoClube.ACEITA, AGORA, OUTRO_USUARIO);
        repository.saveAndFlush(new SolicitacaoEntradaClube(OUTRO_CLUBE, USUARIO, AGORA));
        // IDs fixos tornam o desempate reproduzível, independentemente do UUID gerado pelo JPA.
        UUID menor = UUID.fromString("79000000-0000-0000-0000-000000000001");
        UUID maior = UUID.fromString("79000000-0000-0000-0000-000000000002");
        jdbc.update(
                "UPDATE solicitacao_entrada_clube SET id = ? WHERE id = ?",
                maior,
                primeiro.getId());
        jdbc.update(
                "UPDATE solicitacao_entrada_clube SET id = ? WHERE id = ?", menor, segundo.getId());
        em.clear();

        assertThat(repository.listarPendentes(CLUBE))
                .extracting(SolicitacaoEntradaClube::getId)
                .containsExactly(menor, maior, maisNovo.getId());
        assertThat(repository.contarPendentes(CLUBE)).isEqualTo(3);
        assertThat(repository.contarPendentes(OUTRO_CLUBE)).isEqualTo(1);
        assertThat(repository.buscarPendente(CLUBE, respondido.getUsuarioId())).isEmpty();
        assertThat(repository.buscarComLock(menor, CLUBE)).isPresent();
        assertThat(repository.buscarComLock(menor, OUTRO_CLUBE)).isEmpty();
    }

    @Test
    void banco_rejeita_pendente_duplicado() {
        var tx = transacaoIsolada();
        assertThatThrownBy(
                        () ->
                                tx.executeWithoutResult(
                                        status -> {
                                            repository.saveAndFlush(
                                                    new SolicitacaoEntradaClube(
                                                            CLUBE, USUARIO, AGORA));
                                            repository.saveAndFlush(
                                                    new SolicitacaoEntradaClube(
                                                            CLUBE, USUARIO, AGORA.plusHours(1)));
                                        }))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_solicitacao_entrada_clube_pendente");
    }

    @ParameterizedTest
    @CsvSource({
        "pendente,true,false", "pendente,false,true", "pendente,true,true",
        "aceita,false,false", "aceita,true,false", "aceita,false,true",
        "recusada,false,false", "recusada,true,false", "recusada,false,true"
    })
    void banco_rejeita_resposta_inconsistente(
            String situacao, boolean data, boolean administrador) {
        assertThatThrownBy(
                        () ->
                                transacaoIsolada()
                                        .executeWithoutResult(
                                                status ->
                                                        jdbc.update(
                                                                """
                        INSERT INTO solicitacao_entrada_clube
                            (id, clube_id, usuario_id, situacao, criada_em, respondida_em, administrador_id)
                        VALUES (?, ?, ?, ?::situacao_solicitacao_clube, ?, ?, ?)
                        """,
                                                                UUID.randomUUID(),
                                                                CLUBE,
                                                                USUARIO,
                                                                situacao,
                                                                AGORA,
                                                                data ? AGORA : null,
                                                                administrador
                                                                        ? OUTRO_USUARIO
                                                                        : null)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_solicitacao_entrada_clube_resposta");
    }

    @ParameterizedTest
    @CsvSource({"situacao", "criada_em", "clube_id", "usuario_id"})
    void banco_exige_campos_obrigatorios(String coluna) {
        assertThatThrownBy(
                        () ->
                                transacaoIsolada()
                                        .executeWithoutResult(
                                                status -> {
                                                    UUID id = UUID.randomUUID();
                                                    jdbc.update(
                                                            """
                    INSERT INTO solicitacao_entrada_clube (id, clube_id, usuario_id, situacao, criada_em)
                    VALUES (?, ?, ?, 'pendente', ?)
                    """,
                                                            id,
                                                            CLUBE,
                                                            USUARIO,
                                                            AGORA);
                                                    // Nome restrito aos valores constantes do
                                                    // CsvSource.
                                                    jdbc.update(
                                                            "UPDATE solicitacao_entrada_clube SET "
                                                                    + coluna
                                                                    + " = NULL WHERE id = ?",
                                                            id);
                                                }))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @CsvSource({"clube_id", "usuario_id", "administrador_id"})
    void banco_exige_referencias_existentes(String coluna) {
        assertThatThrownBy(
                        () ->
                                transacaoIsolada()
                                        .executeWithoutResult(
                                                status -> {
                                                    UUID id = UUID.randomUUID();
                                                    jdbc.update(
                                                            """
                    INSERT INTO solicitacao_entrada_clube
                        (id, clube_id, usuario_id, situacao, criada_em, respondida_em, administrador_id)
                    VALUES (?, ?, ?, 'aceita', ?, ?, ?)
                    """,
                                                            id,
                                                            CLUBE,
                                                            USUARIO,
                                                            AGORA,
                                                            AGORA,
                                                            OUTRO_USUARIO);
                                                    jdbc.update(
                                                            "UPDATE solicitacao_entrada_clube SET "
                                                                    + coluna
                                                                    + " = ? WHERE id = ?",
                                                            UUID.randomUUID(),
                                                            id);
                                                }))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_solicitacao_entrada_clube_");
    }

    private TransactionTemplate transacaoIsolada() {
        var tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx;
    }
}
