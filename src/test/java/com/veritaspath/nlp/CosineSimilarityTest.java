package com.veritaspath.nlp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CosineSimilarityTest {

    @Test
    void identicalStringsAreFullySimilar() {
        assertThat(CosineSimilarity.levenshteinSimilarity("hello world", "hello world")).isEqualTo(1.0);
    }

    @Test
    void completelyDifferentStringsScoreLow() {
        double sim = CosineSimilarity.levenshteinSimilarity("abcdef", "zyxwvu");
        assertThat(sim).isLessThan(0.2);
    }

    @Test
    void minorWordingChangeScoresHigh() {
        double sim = CosineSimilarity.levenshteinSimilarity(
                "We are still in an active search phase",
                "We're still in an active search phase"
        );
        assertThat(sim).isGreaterThan(0.85);
    }

    @Test
    void caseIsIgnored() {
        assertThat(CosineSimilarity.levenshteinSimilarity("Hello", "hello")).isEqualTo(1.0);
    }
}
