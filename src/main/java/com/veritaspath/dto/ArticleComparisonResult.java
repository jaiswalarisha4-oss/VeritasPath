package com.veritaspath.dto;

import com.veritaspath.model.DimensionScore;

import java.util.List;

/** Result of comparing one outlet's article against the reference article. */
public record ArticleComparisonResult(
        String outletName,
        double alignmentScore,
        List<DimensionScore> dimensions,
        int referenceSentenceCount,
        int comparisonSentenceCount
) {
}
