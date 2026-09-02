package com.veritaspath.nlp;

import com.veritaspath.model.Quote;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteExtractorTest {

    @Test
    void extractsStraightQuotes() {
        List<Quote> quotes = QuoteExtractor.extract(
                "The mayor said, \"We are doing everything we can to help residents.\" Crews responded quickly.");
        assertThat(quotes).hasSize(1);
        assertThat(quotes.get(0).text()).isEqualTo("We are doing everything we can to help residents.");
    }

    @Test
    void extractsCurlyQuotes() {
        List<Quote> quotes = QuoteExtractor.extract(
                "She said “this was an unprecedented event for our town” during the briefing.");
        assertThat(quotes).hasSize(1);
        assertThat(quotes.get(0).text()).isEqualTo("this was an unprecedented event for our town");
    }

    @Test
    void ignoresVeryShortScareQuotes() {
        List<Quote> quotes = QuoteExtractor.extract("Officials called the response \"fast\".");
        assertThat(quotes).isEmpty();
    }

    @Test
    void returnsEmptyListForNoQuotes() {
        assertThat(QuoteExtractor.extract("A plain sentence with no quoted material at all.")).isEmpty();
    }
}
