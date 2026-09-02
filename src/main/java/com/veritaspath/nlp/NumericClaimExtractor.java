package com.veritaspath.nlp;

import com.veritaspath.model.NumericClaim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts numeric claims (death tolls, percentages, dollar figures,
 * counts) from sentences and normalizes them to a comparable double so
 * "2.3 million" and "2,300,000" resolve to the same value.
 */
public final class NumericClaimExtractor {

    private static final Map<String, Double> MULTIPLIERS = Map.of(
            "thousand", 1_000d,
            "million", 1_000_000d,
            "billion", 1_000_000_000d,
            "trillion", 1_000_000_000_000d
    );

    // A short number (1-2 digits) only counts as a claim when it's next to
    // one of these — otherwise it's too likely to be noise (a page number,
    // "day 5", list markers) rather than a reportable fact.
    private static final Set<String> COUNT_CONTEXT_WORDS = Set.of(
            "people", "person", "killed", "dead", "died", "deaths", "injured",
            "wounded", "residents", "victims", "structures", "homes", "acres",
            "firefighters", "officers", "years", "dollars", "percent", "cases",
            "students", "workers", "employees", "families"
    );

    // [currency]digits[,digits]*[.digits] [multiplier]? [%]?
    private static final Pattern NUMBER_PATTERN = Pattern.compile(
            "([$€£]?)(\\d{1,3}(?:,\\d{3})+|\\d+)(\\.\\d+)?\\s?(thousand|million|billion|trillion)?(%)?",
            Pattern.CASE_INSENSITIVE
    );

    // AP style commonly spells out small numbers ("twelve people died").
    // Only counted as a claim when followed by a count-context word, same
    // rule as short digit numbers, since "one" and "a" are common non-numeric words.
    private static final Map<String, Double> WORD_NUMBERS = Map.ofEntries(
            Map.entry("one", 1d), Map.entry("two", 2d), Map.entry("three", 3d),
            Map.entry("four", 4d), Map.entry("five", 5d), Map.entry("six", 6d),
            Map.entry("seven", 7d), Map.entry("eight", 8d), Map.entry("nine", 9d),
            Map.entry("ten", 10d), Map.entry("eleven", 11d), Map.entry("twelve", 12d),
            Map.entry("thirteen", 13d), Map.entry("fourteen", 14d), Map.entry("fifteen", 15d),
            Map.entry("sixteen", 16d), Map.entry("seventeen", 17d), Map.entry("eighteen", 18d),
            Map.entry("nineteen", 19d), Map.entry("twenty", 20d), Map.entry("thirty", 30d),
            Map.entry("forty", 40d), Map.entry("fifty", 50d), Map.entry("sixty", 60d),
            Map.entry("seventy", 70d), Map.entry("eighty", 80d), Map.entry("ninety", 90d)
    );

    private static final Pattern WORD_NUMBER_PATTERN = Pattern.compile(
            "\\b(" + String.join("|", WORD_NUMBERS.keySet()) + ")\\b", Pattern.CASE_INSENSITIVE);

    private NumericClaimExtractor() {
    }

    public static List<NumericClaim> extract(String articleText) {
        List<NumericClaim> claims = new ArrayList<>();
        if (articleText == null || articleText.isBlank()) {
            return claims;
        }
        for (String sentence : TextUtils.splitSentences(articleText)) {
            extractDigitClaims(sentence, claims);
            extractWordNumberClaims(sentence, claims);
        }
        return claims;
    }

    private static void extractDigitClaims(String sentence, List<NumericClaim> claims) {
        Matcher m = NUMBER_PATTERN.matcher(sentence);
        while (m.find()) {
            String rawInt = m.group(2).replace(",", "");
            String rawDecimal = m.group(3);
            String multiplierWord = m.group(4);
            String percentSign = m.group(5);

            // Skip bare 1-2 digit numbers with no other signal — too
            // noisy (page numbers, list markers, "a 5-year deal" etc.)
            // unless a count-noun right after it (e.g. "12 people") makes
            // it clearly a reportable figure.
            boolean hasSignal = multiplierWord != null || percentSign != null
                    || rawInt.length() >= 3 || (m.group(1) != null && !m.group(1).isEmpty())
                    || hasCountContext(sentence, m.end());
            if (!hasSignal) {
                continue;
            }

            double value = Double.parseDouble(rawInt + (rawDecimal != null ? rawDecimal : ""));
            if (multiplierWord != null) {
                value *= MULTIPLIERS.get(multiplierWord.toLowerCase());
            }
            claims.add(new NumericClaim(m.group().trim(), value, sentence));
        }
    }

    private static void extractWordNumberClaims(String sentence, List<NumericClaim> claims) {
        Matcher m = WORD_NUMBER_PATTERN.matcher(sentence);
        while (m.find()) {
            if (hasCountContext(sentence, m.end())) {
                double value = WORD_NUMBERS.get(m.group(1).toLowerCase());
                claims.add(new NumericClaim(m.group(1), value, sentence));
            }
        }
    }

    private static boolean hasCountContext(String sentence, int matchEnd) {
        String after = sentence.substring(matchEnd, Math.min(sentence.length(), matchEnd + 20));
        Matcher wordMatcher = Pattern.compile("^\\s*([A-Za-z]+)").matcher(after);
        if (!wordMatcher.find()) {
            return false;
        }
        return COUNT_CONTEXT_WORDS.contains(wordMatcher.group(1).toLowerCase());
    }
}
