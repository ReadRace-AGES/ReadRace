package com.readrace.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.Cargo;
import com.readrace.api.model.ClubeDoLivro;
import com.readrace.api.model.MembroClube;

public interface MembroClubeRepository extends JpaRepository<MembroClube, UUID> {

    String RANKING_DO_CLUBE =
            "select u.id as usuarioId, u.nome as nome, u.avatarUrl as avatarUrl,"
                    + " mc.pontos as pontos, rank() over (order by mc.pontos desc) as posicao"
                    + " from MembroClube mc, Usuario u"
                    + " where mc.clube.id = :clubeId"
                    + " and u.id = mc.usuarioId and u.excluidoEm is null"
                    + " order by mc.pontos desc, u.nome asc, u.id asc";

    Optional<MembroClube> findByClube_IdAndUsuarioId(UUID clubeId, UUID usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT membro FROM MembroClube membro WHERE membro.clube.id = :clubeId"
                    + " ORDER BY membro.id")
    List<MembroClube> buscarDoClubeComLock(@Param("clubeId") UUID clubeId);

    @Query(
            "SELECT membro FROM MembroClube membro JOIN Usuario usuario"
                    + " ON usuario.id = membro.usuarioId AND usuario.excluidoEm IS NULL"
                    + " WHERE membro.clube.id = :clubeId"
                    + " AND membro.usuarioId <> :liderId"
                    + " ORDER BY membro.pontos DESC, membro.entrouEm ASC, membro.usuarioId ASC")
    List<MembroClube> candidatosALideranca(
            @Param("clubeId") UUID clubeId, @Param("liderId") UUID liderId, Pageable limite);

    // Responder o quiz soma pontos nesta linha. O lock serializa respostas simultâneas do mesmo
    // membro, para a checagem de "já respondida" e a soma de pontos não correrem em paralelo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT membro
            FROM MembroClube membro
            WHERE membro.clube.id = :clubeId
              AND membro.usuarioId = :usuarioId
            """)
    Optional<MembroClube> buscarComLock(
            @Param("clubeId") UUID clubeId, @Param("usuarioId") UUID usuarioId);

    // Registrar leitura soma pontos nas linhas do usuário em todos os clubes cujo livro atual é o
    // livro registrado (#163). O lock é o mesmo do quiz, para as duas somas na mesma linha não se
    // sobrescreverem; a ordem por id faz transações diferentes pegarem os locks na mesma sequência.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT membro
            FROM MembroClube membro
            WHERE membro.usuarioId = :usuarioId
              AND membro.clube.livro.id = :livroId
              AND membro.clube.excluidoEm IS NULL
            ORDER BY membro.id
            """)
    List<MembroClube> buscarDoLivroComLock(
            @Param("usuarioId") UUID usuarioId, @Param("livroId") UUID livroId);

    @Query(
            "select mc.clube from MembroClube mc"
                    + " where mc.usuarioId = :usuarioId and mc.clube.excluidoEm is null"
                    + " order by mc.clube.nome")
    List<ClubeDoLivro> clubesDoUsuario(@Param("usuarioId") UUID usuarioId);

    @Query(
            "select mc.clube.id as id, count(mc) as total from MembroClube mc"
                    + " where mc.clube.excluidoEm is null"
                    + " group by mc.clube.id")
    List<ContagemMembros> contarMembrosPorClube();

    /**
     * Ranking de um clube, do maior para o menor número de {@code Pontos}. Quem chama limita o
     * tamanho pelo {@code Pageable} — a Página do clube (#35) mostra 7.
     *
<<<<<<< HEAD
        * <p>A posição considera só Pontos, antes da paginação: empates recebem 1, 1, 3. Nome e UUID
        * estabilizam somente a ordem de apresentação entre participantes empatados.
=======
     * <p>A posição considera só Pontos, antes da paginação: empates recebem 1, 1, 3. Nome e UUID
     * estabilizam somente a ordem de apresentação entre participantes empatados.
>>>>>>> c069aceb96b8b8c97717e288340e71e5b100277b
     */
    @Query(RANKING_DO_CLUBE)
    List<LinhaRankingClube> rankingDoClube(@Param("clubeId") UUID clubeId, Pageable limite);

    @Query(RANKING_DO_CLUBE)
    Slice<LinhaRankingClube> paginaDoRanking(@Param("clubeId") UUID clubeId, Pageable pagina);

    long countByClube_Id(UUID clubeId);

    long countByClube_IdAndCargoClube(UUID clubeId, Cargo cargoClube);
}
