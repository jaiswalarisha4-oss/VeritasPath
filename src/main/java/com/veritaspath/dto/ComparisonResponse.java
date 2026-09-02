package com.veritaspath.dto;

import java.time.Instant;
import java.util.List;

/** Top-level API response: the reference article plus a scored result per outlet compared against it. */
public record ComparisonResponse(
        Long id,
        String referenceOutlet,
        Instant createdAt,
        List<ArticleComparisonResult> results
) {
}
