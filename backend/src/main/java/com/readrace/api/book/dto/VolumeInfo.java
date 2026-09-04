package com.readrace.api.book.dto;

import java.util.List;

/**
 * Metadados de um volume (livro).
 *
 * <p>Espelha o campo {@code volumeInfo} da Google Books API. Contém título, autores, editora, data
 * de publicação, descrição, contagem de páginas, categorias, links de imagem, idioma e
 * identificadores de indústria.
 */
public record VolumeInfo(
        String title,
        String subtitle,
        List<String> authors,
        String publisher,
        String publishedDate,
        String description,
        List<IndustryIdentifier> industryIdentifiers,
        Integer pageCount,
        String printType,
        List<String> categories,
        Double averageRating,
        Integer ratingsCount,
        ImageLinks imageLinks,
        String language,
        String previewLink,
        String infoLink,
        String canonicalVolumeLink) {}
