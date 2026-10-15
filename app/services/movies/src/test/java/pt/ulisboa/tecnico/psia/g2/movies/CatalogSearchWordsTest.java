package pt.ulisboa.tecnico.psia.g2.movies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class CatalogSearchWordsTest {

    @Test
    void wordsAreLowercaseLettersAndDigitsOnly() {
        assertEquals(List.of("the", "matrix"), CatalogRepository.words("  The MATRIX "));
        assertEquals(List.of("amélie"), CatalogRepository.words("Amélie"));
        assertEquals(List.of("2001", "a", "space"), CatalogRepository.words("2001: a space"));
    }

    @Test
    void punctuationNeverReachesTheQuery() {
        assertEquals(List.of("a", "b", "c"), CatalogRepository.words("a.*(b)|c\\"));
        assertEquals(List.of(), CatalogRepository.words(".*"));
        assertEquals(List.of(), CatalogRepository.words(null));
    }

    @Test
    void atMostSixWords() {
        assertEquals(CatalogRepository.MAX_SEARCH_WORDS, CatalogRepository.words("a b c d e f g h").size());
    }

    @Test
    void aLeadingArticleIsDroppedUnlessItIsTheOnlyWord() {
        assertEquals(List.of("matrix"), CatalogRepository.withoutLeadingArticle(List.of("the", "matrix")));
        assertEquals(List.of("the"), CatalogRepository.withoutLeadingArticle(List.of("the")));
        assertEquals(List.of("par"), CatalogRepository.withoutLeadingArticle(List.of("par")));
    }
}
