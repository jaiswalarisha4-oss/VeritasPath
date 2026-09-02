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

    // The next two document a real finding from validating against the STS
    // Benchmark (see docs/EXTERNAL_VALIDATION.md): Levenshtein similarity
    // cannot cleanly separate "same meaning, different words" from
    // "different meaning, coincidentally similar structure" -- both land in
    // a similar mid-range band. That's why Quote Fidelity's 0.5 "missing"
    // threshold was deliberately NOT lowered after that validation, even
    // though it misclassifies some true paraphrases as "missing": lowering
    // it would also pull unrelated pairs like the second case below into
    // "altered," which is a worse error (it implies a match was found).

    @Test
    void genuineParaphraseLandsInModerateRangeNotHigh() {
        // STS Benchmark gold score 4.8/5 (humans: "completely equivalent").
        double sim = CosineSimilarity.levenshteinSimilarity(
                "The lady peeled the potatoe.",
                "A woman is peeling a potato."
        );
        assertThat(sim).isBetween(0.35, 0.55);
    }

    @Test
    void unrelatedSentencesWithSimilarStructureAlsoScoreModerately() {
        // Two sentences about different, unrelated topics that happen to
        // share a common template ("A/The X is Y-ing") still land in a
        // similar band to the genuine paraphrase above -- the reason a
        // lower "missing" threshold isn't a safe fix on its own.
        double sim = CosineSimilarity.levenshteinSimilarity(
                "The dog is running in the park.",
                "A man is reading a newspaper."
        );
        assertThat(sim).isLessThan(0.55);
    }
}
