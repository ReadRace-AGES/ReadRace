package com.readrace.api.book.adapter.local;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta uma query no formato da Google Books API, separando os qualificadores de campo.
 *
 * <p>A Google Books API aceita qualificadores especiais dentro do parâmetro {@code q}:
 *
 * <ul>
 *   <li>{@code intitle:} — restringe a busca ao título
 *   <li>{@code inauthor:} — restringe a busca ao autor
 *   <li>{@code subject:} — restringe a busca ao gênero/categoria
 * </ul>
 *
 * <p>Exemplos:
 *
 * <pre>
 *   "intitle:senhor dos aneis"     → apenas no título
 *   "inauthor:tolkien"             → apenas no autor
 *   "subject:fantasia"             → apenas no gênero
 *   "intitle:harry inauthor:rowling" → título E autor
 *   "tolkien"                      → busca geral (todos os campos)
 * </pre>
 *
 * <p>Esta classe existe para que o {@code LocalBooksAdapter} reproduza fielmente o comportamento do
 * Google Books. No {@code GoogleBooksAdapter} a query é repassada como está — o Google já entende
 * os qualificadores nativamente.
 */
final class SearchQuery {

    private static final Pattern QUALIFIER_PATTERN =
            Pattern.compile("(intitle|inauthor|subject):\"?([^\"]+?)\"?(?=\\s+\\w+:|$)");

    private final List<String> titleTerms;
    private final List<String> authorTerms;
    private final List<String> subjectTerms;
    private final String freeText;

    private SearchQuery(
            List<String> titleTerms,
            List<String> authorTerms,
            List<String> subjectTerms,
            String freeText) {
        this.titleTerms = titleTerms;
        this.authorTerms = authorTerms;
        this.subjectTerms = subjectTerms;
        this.freeText = freeText;
    }

    /** Faz o parse de uma query bruta, extraindo os qualificadores de campo. */
    static SearchQuery parse(String rawQuery) {
        List<String> titles = new ArrayList<>();
        List<String> authors = new ArrayList<>();
        List<String> subjects = new ArrayList<>();

        String remaining = rawQuery;

        Matcher matcher = QUALIFIER_PATTERN.matcher(rawQuery);
        while (matcher.find()) {
            String qualifier = matcher.group(1);
            String value = matcher.group(2).trim();

            switch (qualifier) {
                case "intitle" -> titles.add(value);
                case "inauthor" -> authors.add(value);
                case "subject" -> subjects.add(value);
                default -> {
                    // ignora qualificadores desconhecidos
                }
            }

            remaining = remaining.replace(matcher.group(), "");
        }

        String freeText = remaining.trim();

        return new SearchQuery(titles, authors, subjects, freeText);
    }

    /** Indica se a query não usa nenhum qualificador (busca geral em todos os campos). */
    boolean isGeneralSearch() {
        return titleTerms.isEmpty() && authorTerms.isEmpty() && subjectTerms.isEmpty();
    }

    List<String> titleTerms() {
        return titleTerms;
    }

    List<String> authorTerms() {
        return authorTerms;
    }

    List<String> subjectTerms() {
        return subjectTerms;
    }

    String freeText() {
        return freeText;
    }
}
