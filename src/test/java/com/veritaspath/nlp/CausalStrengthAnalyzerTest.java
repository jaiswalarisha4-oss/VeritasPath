package com.veritaspath.nlp;

import com.veritaspath.model.CausalSignal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CausalStrengthAnalyzerTest {

    @Test
    void detectsStrongCausalLanguage() {
        List<CausalSignal> signals = CausalStrengthAnalyzer.extract("Researchers say the chemical causes the disease.");
        assertThat(signals).hasSize(1);
        assertThat(signals.get(0).strength()).isEqualTo(CausalSignal.STRONG);
    }

    @Test
    void detectsWeakCorrelationalLanguage() {
        List<CausalSignal> signals = CausalStrengthAnalyzer.extract("The chemical is associated with higher rates of the disease.");
        assertThat(signals).hasSize(1);
        assertThat(signals.get(0).strength()).isEqualTo(CausalSignal.WEAK);
    }

    @Test
    void detectsModerateLanguage() {
        List<CausalSignal> signals = CausalStrengthAnalyzer.extract("Poor maintenance leads to structural failure over time.");
        assertThat(signals).hasSize(1);
        assertThat(signals.get(0).strength()).isEqualTo(CausalSignal.MODERATE);
    }

    @Test
    void sentencesWithNoCausalLanguageProduceNoSignal() {
        assertThat(CausalStrengthAnalyzer.extract("The bridge is 40 years old and spans the river.")).isEmpty();
    }

    @Test
    void detectsBarePastTenseCaused() {
        List<CausalSignal> signals = CausalStrengthAnalyzer.extract("Heavy demand caused the price spike.");
        assertThat(signals).hasSize(1);
        assertThat(signals.get(0).strength()).isEqualTo(CausalSignal.STRONG);
    }

    // The following keywords were added after validating against real
    // SemEval-2020 Task 11 (PTC v2) data -- see docs/EXTERNAL_VALIDATION.md.

    @Test
    void detectsResponsibleForAsStrongBlameAttribution() {
        List<CausalSignal> signals = CausalStrengthAnalyzer.extract("Officials said the group was responsible for the outbreak.");
        assertThat(signals).hasSize(1);
        assertThat(signals.get(0).strength()).isEqualTo(CausalSignal.STRONG);
    }

    @Test
    void detectsGaveRiseToAsStrong() {
        List<CausalSignal> signals = CausalStrengthAnalyzer.extract("The policy change gave rise to widespread confusion.");
        assertThat(signals).hasSize(1);
        assertThat(signals.get(0).strength()).isEqualTo(CausalSignal.STRONG);
    }

    @Test
    void detectsFueledAsModerate() {
        List<CausalSignal> signals = CausalStrengthAnalyzer.extract("The rumor was fueled by anonymous social media posts.");
        assertThat(signals).hasSize(1);
        assertThat(signals.get(0).strength()).isEqualTo(CausalSignal.MODERATE);
    }

    @Test
    void doesNotFlagGenericBecauseOf() {
        // Deliberately not a keyword: too common in ordinary explanatory
        // prose to serve as a reliable causal-overreach signal on its own.
        assertThat(CausalStrengthAnalyzer.extract("The flight was delayed because of weather.")).isEmpty();
    }
}
