package com.readrace.api.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.ItemBiblioteca;

import jakarta.persistence.LockModeType;

public interface ItemBibliotecaRepository extends JpaRepository<ItemBiblioteca, UUID> {

    /**
     * Um item de uma pagina da biblioteca, na ordem da lista. A atividade e o ultimo registro de
     * leitura ou, sem registro, a data de adicao; vem como {@link Instant} porque e assim que o
     * driver entrega um {@code timestamptz} em query nativa.
     */
    interface ItemPaginado {
        UUID getItemId();

        Instant getAtividade();
    }

    // Native de proposito: RegistroLeitura tambem tem entidade na #33, mas o keyset precisa da
    // ordenacao no banco. A pagina vem so com ids; as entidades sao carregadas depois por findByIdIn,
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

    // Ordem: atividade mais recente primeiro; empate pelo id ascendente. O cursor e o ultimo item
    // devolvido, e a condicao pega quem vem depois dele nessa mesma ordem.
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ItemBiblioteca> findByUsuarioIdAndLivro_Id(UUID usuarioId, UUID livroId);
}
