package com.readrace.api.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.readrace.api.model.ItemBiblioteca;

public interface ItemBibliotecaRepository extends JpaRepository<ItemBiblioteca, UUID> {

    /**
     * Data do último registro de leitura de um item; itens sem registro não aparecem. Vem como
     * {@link Instant} porque é assim que o driver entrega um {@code timestamptz} em query nativa.
     */
    interface UltimaLeitura {
        UUID getItemId();

        Instant getRegistradoEm();
    }

    @EntityGraph(attributePaths = {"livro", "livro.livroAutores", "livro.livroAutores.autor"})
    List<ItemBiblioteca> findByUsuarioId(UUID usuarioId);

    // Native de propósito: RegistroLeitura não tem entidade ainda (é escopo da #33) e a tela só
    // precisa da data mais recente por item para ordenar por atividade.
    @Query(
            value =
                    """
                    SELECT item_biblioteca_id AS itemId, MAX(registrado_em) AS registradoEm
                    FROM registro_leitura
                    WHERE item_biblioteca_id IN (:itemIds)
                    GROUP BY item_biblioteca_id
                    """,
            nativeQuery = true)
    List<UltimaLeitura> buscarUltimaLeituraPorItem(@Param("itemIds") Collection<UUID> itemIds);
}
