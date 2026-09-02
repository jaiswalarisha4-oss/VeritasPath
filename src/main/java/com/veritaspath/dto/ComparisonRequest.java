package com.veritaspath.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Request body: one reference (ground-truth-ish) article vs. one or more outlet articles covering the same story. */
public class ComparisonRequest {

    @NotNull
    @Valid
    private ArticleInput reference;

    @NotEmpty(message = "at least one comparison article is required")
    @Valid
    private List<ArticleInput> comparisons;

    public ArticleInput getReference() {
        return reference;
    }

    public void setReference(ArticleInput reference) {
        this.reference = reference;
    }

    public List<ArticleInput> getComparisons() {
        return comparisons;
    }

    public void setComparisons(List<ArticleInput> comparisons) {
        this.comparisons = comparisons;
    }
}
