package com.veritaspath.model;

/**
 * A causal-language signal found in a sentence, e.g. "smoking causes cancer"
 * (strength 3) vs. "smoking is linked to cancer" (strength 1). Used to
 * detect causal overreach when the same claim is matched across articles.
 */
public record CausalSignal(String sentence, String keyword, int strength) {

    public static final int WEAK = 1;     // "linked to", "associated with"
    public static final int MODERATE = 2; // "leads to", "results in", "triggers"
    public static final int STRONG = 3;   // "causes", "proves", "confirms"

    public String strengthLabel() {
        return switch (strength) {
            case STRONG -> "strong (causal)";
            case MODERATE -> "moderate";
            case WEAK -> "weak (correlational)";
            default -> "none";
        };
    }
}
