package com.readrace.api.book.dto;

/**
 * Identificador de indústria (ISBN_10, ISBN_13, etc.).
 *
 * <p>Espelha o campo {@code volumeInfo.industryIdentifiers[]} da Google Books API.
 */
public record IndustryIdentifier(String type, String identifier) {}
