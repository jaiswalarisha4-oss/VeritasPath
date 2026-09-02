package com.veritaspath.nlp;

import com.veritaspath.model.NumericClaim;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NumericClaimExtractorTest {

    @Test
    void extractsPlainIntegerWithEnoughDigits() {
        List<NumericClaim> claims = NumericClaimExtractor.extract("The fire has burned 8400 acres so far.");
        assertThat(claims).extracting(NumericClaim::normalizedValue).contains(8400.0);
    }

    @Test
    void extractsCommaFormattedNumber() {
        List<NumericClaim> claims = NumericClaimExtractor.extract("Damages are estimated at 1,800,000 dollars.");
        assertThat(claims).extracting(NumericClaim::normalizedValue).contains(1_800_000.0);
    }

    @Test
    void extractsMillionMultiplier() {
        List<NumericClaim> claims = NumericClaimExtractor.extract("Repairs will cost $4.2 million according to officials.");
        assertThat(claims).extracting(NumericClaim::normalizedValue).contains(4_200_000.0);
    }

    @Test
    void extractsPercentage() {
        List<NumericClaim> claims = NumericClaimExtractor.extract("The fire was 15% contained as of Wednesday.");
        assertThat(claims).extracting(NumericClaim::normalizedValue).contains(15.0);
    }

    @Test
    void extractsSpelledOutNumberWithCountContext() {
        List<NumericClaim> claims = NumericClaimExtractor.extract("Twelve people were confirmed dead in the collapse.");
        assertThat(claims).extracting(NumericClaim::normalizedValue).contains(12.0);
    }

    @Test
    void ignoresSpelledOutNumberWithoutCountContext() {
        List<NumericClaim> claims = NumericClaimExtractor.extract("One thing is clear: the bridge needs repair.");
        assertThat(claims).isEmpty();
    }

    @Test
    void skipsNoisyShortNumbersWithNoSignal() {
        List<NumericClaim> claims = NumericClaimExtractor.extract("They met on day 5 of the investigation.");
        assertThat(claims).isEmpty();
    }

    @Test
    void extractsSportsScoreWithCountContext() {
        List<NumericClaim> claims = NumericClaimExtractor.extract("Owens scored 28 points in the win.");
        assertThat(claims).extracting(NumericClaim::normalizedValue).contains(28.0);
    }

    @Test
    void extractsWeatherMeasurementWithCountContext() {
        List<NumericClaim> claims = NumericClaimExtractor.extract("The storm will bring 10 inches of rain.");
        assertThat(claims).extracting(NumericClaim::normalizedValue).contains(10.0);
    }
}
