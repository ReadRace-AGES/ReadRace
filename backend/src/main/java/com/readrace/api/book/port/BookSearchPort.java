package com.readrace.api.book.port;

import com.readrace.api.book.dto.GoogleBookVolume;
import com.readrace.api.book.dto.GoogleBooksResponse;

/**
 * Porta de busca de livros.
 *
 * <p>Define o contrato que qualquer provedor de livros deve implementar. O restante da aplicação
 * programa contra esta interface — nunca contra uma implementação concreta.
 *
 * <p>Implementações disponíveis:
 *
 * <ul>
 *   <li>{@code LocalBooksAdapter} — mock local com ~100 livros (profile: dev)
 *   <li>{@code GoogleBooksAdapter} — Google Books API real (profile: prod)
 * </ul>
 *
 * <p>A troca entre implementações é feita via Spring Profiles, sem alterar nenhum código
 * consumidor.
 */
public interface BookSearchPort {

    /**
     * Busca livros por texto livre (título, autor, ISBN, etc.).
     *
     * @param query termo de busca
     * @param maxResults máximo de resultados (1-40, padrão 10)
     * @param startIndex índice do primeiro resultado (paginação, começa em 0)
     * @return resposta no formato Google Books API
     */
    GoogleBooksResponse search(String query, int maxResults, int startIndex);

    /**
     * Busca um volume específico pelo ID.
     *
     * @param volumeId identificador único do volume
     * @return o volume encontrado, ou {@code null} se não existir
     */
    GoogleBookVolume getById(String volumeId);
}
