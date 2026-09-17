package com.readrace.api.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.readrace.api.dto.response.PerfilResponse;

@Repository
public class PerfilRepository {
    private final JdbcTemplate jdbc;

    public PerfilRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Resumo(long seguidores, long seguindo, long livrosLidos, long paginasLidas) {}

    public Resumo resumo(UUID id) {
        return jdbc.queryForObject(
                """
            SELECT (SELECT count(*) FROM seguir WHERE seguido_id = ?) AS seguidores,
                   (SELECT count(*) FROM seguir WHERE seguidor_id = ?) AS seguindo,
                   count(*) FILTER (WHERE status_leitura = 'lido') AS livros,
                   coalesce(sum(pagina_maxima), 0) AS paginas
            FROM item_biblioteca WHERE usuario_id = ?
            """,
                (rs, row) ->
                        new Resumo(
                                rs.getLong("seguidores"),
                                rs.getLong("seguindo"),
                                rs.getLong("livros"),
                                rs.getLong("paginas")),
                id,
                id,
                id);
    }

    public List<PerfilResponse.Conquista> conquistas(UUID id) {
        return jdbc.query(
                """
            SELECT c.id, c.nome, c.icone_url, c.descricao, cu.obtida_em
            FROM conquista c LEFT JOIN conquista_usuario cu
              ON cu.conquista_id = c.id AND cu.usuario_id = ?
            ORDER BY c.criada_em, c.id
            """,
                (rs, row) ->
                        new PerfilResponse.Conquista(
                                rs.getObject("id", UUID.class),
                                rs.getString("nome"),
                                rs.getString("icone_url"),
                                rs.getString("descricao"),
                                rs.getObject("obtida_em") != null,
                                rs.getObject("obtida_em", OffsetDateTime.class)),
                id);
    }

    public List<PerfilResponse.Favorito> favoritos(UUID id) {
        return jdbc.query(
                """
            SELECT l.id, l.titulo, l.capa_url,
              (SELECT string_agg(a.nome, ', ' ORDER BY la.ordem, a.id)
               FROM livro_autor la JOIN autor a ON a.id = la.autor_id
               WHERE la.livro_id = l.id) AS autor
            FROM item_biblioteca i JOIN livro l ON l.id = i.livro_id
            WHERE i.usuario_id = ? AND i.favorito = true
            ORDER BY i.adicionado_em, i.id
            """,
                (rs, row) ->
                        new PerfilResponse.Favorito(
                                rs.getObject("id", UUID.class),
                                rs.getString("titulo"),
                                rs.getString("autor"),
                                rs.getString("capa_url")),
                id);
    }
}
