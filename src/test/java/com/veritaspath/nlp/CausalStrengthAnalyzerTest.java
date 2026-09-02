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
}
