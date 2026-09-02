package com.veritaspath.nlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based text primitives: sentence segmentation, tokenization and
 * stopword filtering. Implemented from scratch (no pretrained model
 * download) so the whole pipeline builds and runs offline on plain text.
 */
public final class TextUtils {

    // Common abbreviations that must not be treated as sentence boundaries.
    // Set.copyOf (rather than Set.of) tolerates accidental duplicates below.
    private static final Set<String> ABBREVIATIONS = Set.copyOf(Arrays.asList(
            "mr", "mrs", "ms", "dr", "prof", "sr", "jr", "st", "vs", "etc",
            "inc", "ltd", "co", "corp", "gov", "u.s", "u.k", "u.n", "e.g", "i.e",
            "no", "gen", "col", "sen", "rep", "capt", "sgt", "lt"
    ));

    private static final Set<String> STOPWORDS = Set.copyOf(Arrays.asList(
            "a", "an", "the", "and", "or", "but", "if", "then", "else", "of", "to",
            "in", "on", "at", "by", "for", "with", "about", "against", "between",
            "into", "through", "during", "before", "after", "above", "below",
            "from", "up", "down", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "having", "do", "does", "did", "doing", "will",
            "would", "should", "could", "can", "shall", "may", "might", "must",
            "this", "that", "these", "those", "it", "its", "as", "he", "she",
            "they", "them", "his", "her", "their", "we", "you", "i", "said",
            "not", "no", "so", "than", "too", "very", "just", "also"
    ));

    private static final Pattern SENTENCE_SPLIT =
            Pattern.compile("(?<=[.!?])\\s+(?=[A-Z\\u201C\"])");

    private static final Pattern WORD = Pattern.compile("[A-Za-z][A-Za-z'-]*|\\d+(?:[.,]\\d+)?%?");

    private TextUtils() {
    }

    /**
     * Splits raw article text into sentences. Uses punctuation boundaries
     * followed by a capital letter or quote, while guarding against a
     * short list of common abbreviations so "Dr. Smith" doesn't split.
     */
    public static List<String> splitSentences(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        String[] roughSplit = SENTENCE_SPLIT.split(normalized);

        StringBuilder buffer = new StringBuilder();
        for (String piece : roughSplit) {
            if (buffer.length() > 0) {
                buffer.append(" ");
            }
            buffer.append(piece);

            String trimmed = buffer.toString().trim();
            String lastWord = lastWordBeforeBoundary(trimmed);
            if (lastWord != null && ABBREVIATIONS.contains(lastWord.toLowerCase())) {
                // Likely a false boundary (e.g. "Dr.") — keep accumulating.
                continue;
            }
            if (trimmed.length() > 0) {
                result.add(trimmed);
                buffer.setLength(0);
            }
        }
        if (buffer.length() > 0) {
            result.add(buffer.toString().trim());
        }
        return result;
    }

    private static String lastWordBeforeBoundary(String sentenceSoFar) {
        Matcher m = Pattern.compile("([A-Za-z]+)\\.$").matcher(sentenceSoFar);
        return m.find() ? m.group(1) : null;
    }

    /** Lower-cased word tokens, punctuation stripped. */
    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        Matcher m = WORD.matcher(text);
        while (m.find()) {
            tokens.add(m.group().toLowerCase());
        }
        return tokens;
    }

    /** Tokenizes and removes stopwords — the vocabulary used for TF-IDF. */
    public static List<String> significantTokens(String text) {
        List<String> out = new ArrayList<>();
        for (String tok : tokenize(text)) {
            if (!STOPWORDS.contains(tok) && tok.length() > 1) {
                out.add(tok);
            }
        }
        return out;
    }
}
