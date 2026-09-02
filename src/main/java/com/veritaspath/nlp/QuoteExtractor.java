package com.veritaspath.nlp;

import com.veritaspath.model.Quote;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls directly-quoted spans out of article text using straight and
 * curly double quotes. Each quote is paired with its containing sentence
 * so a later diff can show attribution context ("according to...").
 */
public final class QuoteExtractor {

    // Matches "..." or “...” with at least 4 characters inside, so it
    // skips single-word scare quotes like "unprecedented" that aren't
    // really attributed speech.
    private static final Pattern QUOTE_PATTERN =
            Pattern.compile("[\"\\u201C]([^\"\\u201D]{4,400})[\"\\u201D]");

    private QuoteExtractor() {
    }

    public static List<Quote> extract(String articleText) {
        List<Quote> quotes = new ArrayList<>();
        if (articleText == null || articleText.isBlank()) {
            return quotes;
        }
        for (String sentence : TextUtils.splitSentences(articleText)) {
            Matcher m = QUOTE_PATTERN.matcher(sentence);
            while (m.find()) {
                String quoteText = m.group(1).trim();
                // Require multiple words so single-word "scare quotes" like
                // "unprecedented" aren't mistaken for attributed speech.
                if (!quoteText.isEmpty() && quoteText.contains(" ")) {
                    quotes.add(new Quote(quoteText, sentence));
                }
            }
        }
        return quotes;
    }
}
