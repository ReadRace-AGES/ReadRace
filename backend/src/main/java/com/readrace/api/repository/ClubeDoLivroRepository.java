package com.readrace.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.ClubeDoLivro;

public interface ClubeDoLivroRepository extends JpaRepository<ClubeDoLivro, UUID> {

    /** Agrega autores e membros antes dos joins, sem multiplicar contagens por autor. */
    @Query(
            value =
                    """
            SELECT c.id, c.nome, c.capa_url AS capaUrl, l.titulo, autores.autor,
                   coalesce(contagem.total, 0) AS membros,
                   CASE WHEN meu.id IS NOT NULL THEN 'membro'
                        WHEN pedido.id IS NOT NULL THEN 'pendente'
                        ELSE 'nenhuma' END AS situacao
            FROM clube_do_livro c
            JOIN livro l ON l.id = c.livro_id
            LEFT JOIN (
                SELECT la.livro_id, string_agg(a.nome, ', ' ORDER BY la.ordem) AS autor
                FROM livro_autor la JOIN autor a ON a.id = la.autor_id
                GROUP BY la.livro_id
            ) autores ON autores.livro_id = l.id
            LEFT JOIN (
                SELECT clube_id, count(*) AS total FROM membro_clube GROUP BY clube_id
            ) contagem ON contagem.clube_id = c.id
            LEFT JOIN membro_clube meu ON meu.clube_id = c.id AND meu.usuario_id = :usuarioId
            LEFT JOIN solicitacao_entrada_clube pedido ON pedido.clube_id = c.id
                AND pedido.usuario_id = :usuarioId AND pedido.situacao = 'pendente'
            WHERE c.excluido_em IS NULL
            ORDER BY c.nome, c.id
            """,
            nativeQuery = true)
    List<LinhaClubeDisponivel> listarAtivos(@Param("usuarioId") UUID usuarioId);

    Optional<ClubeDoLivro> findByIdAndExcluidoEmIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ClubeDoLivro c where c.id = :id and c.excluidoEm is null")
    Optional<ClubeDoLivro> buscarComLock(@Param("id") UUID id);
}
