package com.veritaspath.nlp;

import com.veritaspath.model.CausalSignal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Rule-based detector for causal-strength language. Journalists (and
 * headline writers under deadline pressure) routinely upgrade a study's
 * "associated with" into "causes" — this flags that class of overreach by
 * keyword strength, independent of any specific topic.
 */
public final class CausalStrengthAnalyzer {

    // Ordered so a longer, more specific phrase is checked before a
    // shorter substring of it (e.g. "results in" before "results").
    private static final Map<String, Integer> KEYWORDS = new LinkedHashMap<>();

    static {
        // Strong / causal
        KEYWORDS.put("proven to cause", CausalSignal.STRONG);
        KEYWORDS.put("directly causes", CausalSignal.STRONG);
        KEYWORDS.put("causes", CausalSignal.STRONG);
        KEYWORDS.put("caused by", CausalSignal.STRONG);
        KEYWORDS.put("caused", CausalSignal.STRONG);
        KEYWORDS.put("proves that", CausalSignal.STRONG);
        KEYWORDS.put("confirms that", CausalSignal.STRONG);
        KEYWORDS.put("responsible for", CausalSignal.STRONG);
        KEYWORDS.put("blamed for", CausalSignal.STRONG);
        KEYWORDS.put("to blame for", CausalSignal.STRONG);
        KEYWORDS.put("gave rise to", CausalSignal.STRONG);
        KEYWORDS.put("giving rise to", CausalSignal.STRONG);
        // Moderate
        KEYWORDS.put("leads to", CausalSignal.MODERATE);
        KEYWORDS.put("led to", CausalSignal.MODERATE);
        KEYWORDS.put("results in", CausalSignal.MODERATE);
        KEYWORDS.put("triggers", CausalSignal.MODERATE);
        KEYWORDS.put("drives", CausalSignal.MODERATE);
        KEYWORDS.put("contributes to", CausalSignal.MODERATE);
        KEYWORDS.put("enabled", CausalSignal.MODERATE);
        KEYWORDS.put("fueled", CausalSignal.MODERATE);
        KEYWORDS.put("sparked", CausalSignal.MODERATE);
        KEYWORDS.put("prompted", CausalSignal.MODERATE);
        KEYWORDS.put("set off", CausalSignal.MODERATE);
        // Weak / correlational
        KEYWORDS.put("linked to", CausalSignal.WEAK);
        KEYWORDS.put("associated with", CausalSignal.WEAK);
        KEYWORDS.put("correlated with", CausalSignal.WEAK);
        KEYWORDS.put("may be related to", CausalSignal.WEAK);
        KEYWORDS.put("could be tied to", CausalSignal.WEAK);
        KEYWORDS.put("tied to", CausalSignal.WEAK);
    }

    private CausalStrengthAnalyzer() {
    }

    public static List<CausalSignal> extract(String articleText) {
        List<CausalSignal> signals = new ArrayList<>();
        if (articleText == null || articleText.isBlank()) {
            return signals;
        }
        for (String sentence : TextUtils.splitSentences(articleText)) {
            String lower = sentence.toLowerCase();
            String bestKeyword = null;
            int bestStrength = -1;
            for (Map.Entry<String, Integer> e : KEYWORDS.entrySet()) {
                if (containsWholePhrase(lower, e.getKey()) && e.getValue() > bestStrength) {
                    bestStrength = e.getValue();
                    bestKeyword = e.getKey();
                }
            }
            if (bestKeyword != null) {
                signals.add(new CausalSignal(sentence, bestKeyword, bestStrength));
            }
        }
        return signals;
    }

    private static boolean containsWholePhrase(String haystack, String phrase) {
        return Pattern.compile("\\b" + Pattern.quote(phrase) + "\\b").matcher(haystack).find();
    }
}
