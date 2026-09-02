package com.veritaspath.nlp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TfIdfVectorizerTest {

    @Test
    void identicalSentencesAreFullySimilar() {
        TfIdfVectorizer v = new TfIdfVectorizer(List.of(
                "The bridge collapsed early Tuesday morning.",
                "The bridge collapsed early Tuesday morning."
        ));
        double sim = CosineSimilarity.similarity(v.vectorFor(0), v.vectorFor(1));
        assertThat(sim).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void unrelatedSentencesHaveLowSimilarity() {
        TfIdfVectorizer v = new TfIdfVectorizer(List.of(
                "The bridge collapsed early Tuesday morning near the river.",
                "A new study on energy drinks found higher headache rates."
        ));
        double sim = CosineSimilarity.similarity(v.vectorFor(0), v.vectorFor(1));
        assertThat(sim).isLessThan(0.2);
    }

    @Test
    void paraphrasedSentenceStillScoresModeratelySimilar() {
        TfIdfVectorizer v = new TfIdfVectorizer(List.of(
                "Twelve people were confirmed dead in the bridge collapse.",
                "Officials confirmed twelve people died when the bridge collapsed."
        ));
        double sim = CosineSimilarity.similarity(v.vectorFor(0), v.vectorFor(1));
        assertThat(sim).isGreaterThan(0.3);
    }

    @Test
    void emptyDocumentProducesZeroVector() {
        TfIdfVectorizer v = new TfIdfVectorizer(List.of("", "Some real content here about a fire."));
        double[] empty = v.vectorFor(0);
        for (double d : empty) {
            assertThat(d).isZero();
        }
    }
}
