package com.veritaspath.model;

import java.util.List;

/**
 * The score and human-readable findings for one of the four drift
 * dimensions (quote fidelity, numeric accuracy, omission, causal strength).
 */
public record DimensionScore(String name, double score, List<String> findings) {
}
