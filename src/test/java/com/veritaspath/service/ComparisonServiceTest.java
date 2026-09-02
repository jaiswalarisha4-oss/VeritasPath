package com.veritaspath.service;

import com.veritaspath.dto.ArticleComparisonResult;
import com.veritaspath.model.DimensionScore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ComparisonServiceTest {

    private final ComparisonService service = new ComparisonService(null);

    @Test
    void identicalArticleScoresNearPerfectAcrossAllDimensions() {
        String text = "The bridge collapsed at 6 a.m. Twelve people were confirmed dead. "
                + "\"We are still searching the area,\" said Fire Marshal Elena Ruiz. "
                + "Officials say the collapse is linked to years of delayed maintenance.";

        ArticleComparisonResult result = service.compare(text, "Same Outlet", text);

        assertThat(result.alignmentScore()).isGreaterThanOrEqualTo(99.0);
        result.dimensions().forEach(d -> assertThat(d.score()).isGreaterThanOrEqualTo(99.0));
    }

    @Test
    void numericMismatchLowersNumericAccuracyDimension() {
        String reference = "Officials confirmed 12 people were killed in the collapse. The bridge is 40 years old.";
        String comparison = "Officials confirmed 18 people were killed in the collapse. The bridge is 40 years old.";

        ArticleComparisonResult result = service.compare(reference, "Outlet", comparison);
        DimensionScore numeric = dimension(result, "Numeric Accuracy");

        assertThat(numeric.score()).isLessThan(100.0);
        assertThat(numeric.findings().toString()).contains("not matched");
    }

    @Test
    void alteredQuoteLowersQuoteFidelityDimension() {
        String reference = "\"We are still in an active search phase and I don't want anyone to treat this number as final,\" said the fire marshal.";
        String comparison = "\"This bridge was a disaster waiting to happen,\" the fire marshal said.";

        ArticleComparisonResult result = service.compare(reference, "Outlet", comparison);
        DimensionScore quotes = dimension(result, "Quote Fidelity");

        assertThat(quotes.score()).isLessThan(60.0);
    }

    @Test
    void causalOverreachIsFlagged() {
        String reference = "The chemical is associated with higher rates of the disease, researchers said, "
                + "cautioning that the study does not establish a direct link.";
        String comparison = "The chemical causes the disease, researchers said, warning the public about the risk.";

        ArticleComparisonResult result = service.compare(reference, "Outlet", comparison);
        DimensionScore causal = dimension(result, "Causal-Claim Strength");

        assertThat(causal.score()).isLessThan(100.0);
        assertThat(causal.findings().toString()).contains("overreach");
    }

    @Test
    void droppedCaveatIsFlaggedAsOmission() {
        String reference = "The fire has burned 8400 acres and is 15 percent contained. "
                + "Officials cautioned that the damage estimate is preliminary and subject to significant revision "
                + "once ground assessment teams can safely access the affected area.";
        String comparison = "The fire has burned 8400 acres and is 15 percent contained.";

        ArticleComparisonResult result = service.compare(reference, "Outlet", comparison);
        DimensionScore omission = dimension(result, "Omission of Content");

        assertThat(omission.score()).isLessThan(100.0);
    }

    private DimensionScore dimension(ArticleComparisonResult result, String name) {
        Optional<DimensionScore> found = result.dimensions().stream().filter(d -> d.name().equals(name)).findFirst();
        assertThat(found).as("dimension " + name).isPresent();
        return found.get();
    }
}
