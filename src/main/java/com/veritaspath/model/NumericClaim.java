package com.veritaspath.model;

/**
 * A numeric fact extracted from a sentence (a death toll, a percentage, a
 * dollar figure, a count) along with the multiplier-normalized value so
 * "2.3 million" and "2,300,000" compare equal.
 */
public record NumericClaim(String rawText, double normalizedValue, String context) {
}
