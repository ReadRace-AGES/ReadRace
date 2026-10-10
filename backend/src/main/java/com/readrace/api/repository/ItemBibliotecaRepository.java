package com.readrace.api.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.ItemBiblioteca;

public interface ItemBibliotecaRepository extends JpaRepository<ItemBiblioteca, UUID> {

    /**
     * Um item de uma página da biblioteca, na ordem da lista. A atividade é o último registro de
     * leitura ou, sem registro, a data de adição; vem como {@link Instant} porque é assim que o
     * driver entrega um {@code timestamptz} em query nativa.
     */
    interface ItemPaginado {
        UUID getItemId();

        Instant getAtividade();
    }

    // Native de propósito: RegistroLeitura também tem entidade na #33, mas o keyset precisa da
    // ordenação no banco. A página vem só com ids; as entidades são carregadas depois por
    // findByIdIn,
    // com livro e autores, em uma query.
    String PAGINA_SELECT =
            """
            SELECT i.id AS itemId, GREATEST(r.ultima, i.adicionado_em) AS atividade
            FROM item_biblioteca i
            LEFT JOIN (
                SELECT item_biblioteca_id, MAX(registrado_em) AS ultima
                FROM registro_leitura
                GROUP BY item_biblioteca_id
            ) r ON r.item_biblioteca_id = i.id
            WHERE i.usuario_id = :usuarioId
            """;

    // Ordem: atividade mais recente primeiro; empate pelo id ascendente. O cursor é o último item
    // devolvido, e a condição pega quem vem depois dele nessa mesma ordem.
    String PAGINA_CURSOR_E_ORDEM =
            """
              AND (
                GREATEST(r.ultima, i.adicionado_em) < :cursorAtividade
                OR (GREATEST(r.ultima, i.adicionado_em) = :cursorAtividade AND i.id > :cursorId)
              )
            ORDER BY atividade DESC, i.id ASC
            LIMIT :limite
            """;

    @Query(value = PAGINA_SELECT + "  AND i.favorito\n" + PAGINA_CURSOR_E_ORDEM, nativeQuery = true)
    List<ItemPaginado> paginarFavoritos(
            @Param("usuarioId") UUID usuarioId,
            @Param("cursorAtividade") Instant cursorAtividade,
            @Param("cursorId") UUID cursorId,
            @Param("limite") int limite);

    @Query(
            value =
                    PAGINA_SELECT
                            + "  AND i.status_leitura = CAST(:status AS status_leitura)\n"
                            + PAGINA_CURSOR_E_ORDEM,
            nativeQuery = true)
    List<ItemPaginado> paginarPorStatus(
            @Param("usuarioId") UUID usuarioId,
            @Param("status") String status,
            @Param("cursorAtividade") Instant cursorAtividade,
            @Param("cursorId") UUID cursorId,
            @Param("limite") int limite);

    @EntityGraph(attributePaths = {"livro", "livro.livroAutores", "livro.livroAutores.autor"})
    List<ItemBiblioteca> findByIdIn(Collection<UUID> ids);

    interface GeneroDoLivro {
        UUID getLivroId();

        String getNome();
    }

    @Query(
            value =
                    """
                    SELECT DISTINCT ON (lg.livro_id) lg.livro_id AS livroId, g.nome AS nome
                    FROM livro_genero lg
                    JOIN genero g ON g.id = lg.genero_id
                    WHERE lg.livro_id IN (:livroIds)
                    ORDER BY lg.livro_id, g.nome ASC, g.id ASC
                    """,
            nativeQuery = true)
    List<GeneroDoLivro> buscarPrimeiroGeneroDosLivros(@Param("livroIds") Collection<UUID> livroIds);

    @Modifying
    @Query(
            value =
                    """
            INSERT INTO item_biblioteca (id, usuario_id, livro_id, status_leitura)
            VALUES (:id, :usuarioId, :livroId, 'lendo')
            ON CONFLICT (usuario_id, livro_id) DO NOTHING
            """,
            nativeQuery = true)
    void criarSeAusente(
            @Param("id") UUID id,
            @Param("usuarioId") UUID usuarioId,
            @Param("livroId") UUID livroId);

    // "Lido"/"Desejos" trocam o status, criando o item se ainda não existir (#155). Favorito não é
    // tocado: EXCLUDED não o inclui, então um item existente mantém o que já tinha. O upsert é
    // nativo e o Hibernate não o enxerga: sem clearAutomatically, o findBy seguinte devolveria o
    // item antigo, em cache na mesma transação, em vez do que o banco acabou de gravar.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value =
                    """
            INSERT INTO item_biblioteca (id, usuario_id, livro_id, status_leitura)
            VALUES (:id, :usuarioId, :livroId, CAST(:status AS status_leitura))
            ON CONFLICT (usuario_id, livro_id) DO UPDATE SET status_leitura = EXCLUDED.status_leitura
            """,
            nativeQuery = true)
    void definirStatus(
            @Param("id") UUID id,
            @Param("usuarioId") UUID usuarioId,
            @Param("livroId") UUID livroId,
            @Param("status") String status);

    // "Favoritos" (#155): item novo entra como desejo e favorito; item existente só ganha a flag,
    // sem mudar status nem página. Mesmo motivo do cache de definirStatus.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value =
                    """
            INSERT INTO item_biblioteca (id, usuario_id, livro_id, status_leitura, favorito)
            VALUES (:id, :usuarioId, :livroId, 'desejo', true)
            ON CONFLICT (usuario_id, livro_id) DO UPDATE SET favorito = true
            """,
            nativeQuery = true)
    void marcarFavorito(
            @Param("id") UUID id,
            @Param("usuarioId") UUID usuarioId,
            @Param("livroId") UUID livroId);

    // Consulta sem lock para o endpoint de detalhe, que usa transação somente leitura.
    Optional<ItemBiblioteca> findByUsuarioIdAndLivroId(UUID usuarioId, UUID livroId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ItemBiblioteca> findByUsuarioIdAndLivro_Id(UUID usuarioId, UUID livroId);
}
