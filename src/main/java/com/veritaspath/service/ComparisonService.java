package com.veritaspath.service;

import com.veritaspath.dto.ArticleComparisonResult;
import com.veritaspath.dto.ArticleInput;
import com.veritaspath.model.CausalSignal;
import com.veritaspath.model.DimensionScore;
import com.veritaspath.model.NumericClaim;
import com.veritaspath.model.Quote;
import com.veritaspath.nlp.CausalStrengthAnalyzer;
import com.veritaspath.nlp.CosineSimilarity;
import com.veritaspath.nlp.NumericClaimExtractor;
import com.veritaspath.nlp.QuoteExtractor;
import com.veritaspath.nlp.TextUtils;
import com.veritaspath.nlp.TfIdfVectorizer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core scoring engine. Compares a "reference" article against one or more
 * outlet articles covering the same story across four independent, named
 * dimensions, then rolls them into a single 0-100 alignment score.
 *
 * <p>This is deliberately the "classical ML baseline" tier: TF-IDF cosine
 * similarity and rule-based extraction, no external model downloads. See
 * docs/ARCHITECTURE.md for where a fine-tuned transformer or an LLM judge
 * would slot in as a second/third scoring tier.
 */
@Service
public class ComparisonService {

    private static final double OMISSION_SIMILARITY_THRESHOLD = 0.12;
    private static final int MIN_SENTENCE_LENGTH_FOR_OMISSION_CHECK = 40;
    private static final double QUOTE_INTACT_THRESHOLD = 0.85;
    private static final double QUOTE_ALTERED_THRESHOLD = 0.5;
    private static final double NUMERIC_RELATIVE_TOLERANCE = 0.02; // 2%
    private static final double CAUSAL_TOPIC_MATCH_THRESHOLD = 0.20;

    private static final double WEIGHT_QUOTE = 0.30;
    private static final double WEIGHT_NUMERIC = 0.30;
    private static final double WEIGHT_OMISSION = 0.25;
    private static final double WEIGHT_CAUSAL = 0.15;

    private static final int MAX_FINDINGS_PER_DIMENSION = 6;

    private final ArticleFetchService articleFetchService;

    public ComparisonService(ArticleFetchService articleFetchService) {
        this.articleFetchService = articleFetchService;
    }

    public String resolveText(ArticleInput input) {
        if (StringUtils.hasText(input.getText())) {
            return input.getText();
        }
        if (StringUtils.hasText(input.getUrl())) {
            return articleFetchService.fetchArticleText(input.getUrl());
        }
        throw new IllegalArgumentException("Article '" + input.getOutletName() + "' has neither text nor url");
    }

    public ArticleComparisonResult compare(String referenceText, ArticleInput comparisonInput) {
        String comparisonText = resolveText(comparisonInput);
        return compare(referenceText, comparisonInput.getOutletName(), comparisonText);
    }

    public ArticleComparisonResult compare(String referenceText, String comparisonOutlet, String comparisonText) {
        List<String> refSentences = TextUtils.splitSentences(referenceText);
        List<String> targetSentences = TextUtils.splitSentences(comparisonText);

        DimensionScore omission = scoreOmission(refSentences, targetSentences);
        DimensionScore quoteFidelity = scoreQuoteFidelity(referenceText, comparisonText);
        DimensionScore numericAccuracy = scoreNumericAccuracy(referenceText, comparisonText);
        DimensionScore causalConsistency = scoreCausalConsistency(referenceText, comparisonText, refSentences, targetSentences);

        double composite = WEIGHT_QUOTE * quoteFidelity.score()
                + WEIGHT_NUMERIC * numericAccuracy.score()
                + WEIGHT_OMISSION * omission.score()
                + WEIGHT_CAUSAL * causalConsistency.score();

        List<DimensionScore> dimensions = List.of(quoteFidelity, numericAccuracy, omission, causalConsistency);

        return new ArticleComparisonResult(
                comparisonOutlet,
                round1(composite),
                dimensions,
                refSentences.size(),
                targetSentences.size()
        );
    }

    // ---- Dimension 1: Omission — reference content missing from coverage ----

    private DimensionScore scoreOmission(List<String> refSentences, List<String> targetSentences) {
        List<String> considered = refSentences.stream()
                .filter(s -> s.length() >= MIN_SENTENCE_LENGTH_FOR_OMISSION_CHECK)
                .collect(Collectors.toList());

        if (considered.isEmpty() || targetSentences.isEmpty()) {
            return new DimensionScore("Omission of Content", 100.0,
                    List.of("Not enough sentence data to evaluate omissions."));
        }

        List<String> combined = new ArrayList<>(considered);
        combined.addAll(targetSentences);
        TfIdfVectorizer vectorizer = new TfIdfVectorizer(combined);

        List<String> omitted = new ArrayList<>();
        for (int i = 0; i < considered.size(); i++) {
            double[] refVector = vectorizer.vectorFor(i);
            double best = 0.0;
            for (int j = 0; j < targetSentences.size(); j++) {
                double[] targetVector = vectorizer.vectorFor(considered.size() + j);
                best = Math.max(best, CosineSimilarity.similarity(refVector, targetVector));
            }
            if (best < OMISSION_SIMILARITY_THRESHOLD) {
                omitted.add(considered.get(i));
            }
        }

        double score = 100.0 * (1.0 - ((double) omitted.size() / considered.size()));
        List<String> findings = omitted.stream()
                .limit(MAX_FINDINGS_PER_DIMENSION)
                .map(s -> "Not covered: \"" + truncate(s, 160) + "\"")
                .collect(Collectors.toList());
        if (findings.isEmpty()) {
            findings = List.of("No significant reference content was omitted.");
        }
        return new DimensionScore("Omission of Content", round1(score), findings);
    }

    // ---- Dimension 2: Quote fidelity — are attributed quotes preserved? ----

    private DimensionScore scoreQuoteFidelity(String referenceText, String comparisonText) {
        List<Quote> refQuotes = QuoteExtractor.extract(referenceText);
        List<Quote> targetQuotes = QuoteExtractor.extract(comparisonText);

        if (refQuotes.isEmpty()) {
            return new DimensionScore("Quote Fidelity", 100.0,
                    List.of("Reference article contains no directly quoted material to check."));
        }

        List<String> findings = new ArrayList<>();
        double totalSimilarity = 0.0;

        for (Quote refQuote : refQuotes) {
            double best = 0.0;
            for (Quote targetQuote : targetQuotes) {
                best = Math.max(best, CosineSimilarity.levenshteinSimilarity(refQuote.text(), targetQuote.text()));
            }
            totalSimilarity += best;

            if (best < QUOTE_ALTERED_THRESHOLD) {
                findings.add("Missing or unrecognizable quote: \"" + truncate(refQuote.text(), 140) + "\"");
            } else if (best < QUOTE_INTACT_THRESHOLD) {
                findings.add("Quote appears altered/paraphrased (" + Math.round(best * 100)
                        + "% match): \"" + truncate(refQuote.text(), 140) + "\"");
            }
        }

        double score = 100.0 * (totalSimilarity / refQuotes.size());
        List<String> capped = findings.stream().limit(MAX_FINDINGS_PER_DIMENSION).collect(Collectors.toList());
        if (capped.isEmpty()) {
            capped = List.of("All " + refQuotes.size() + " reference quote(s) were preserved faithfully.");
        }
        return new DimensionScore("Quote Fidelity", round1(score), capped);
    }

    // ---- Dimension 3: Numeric accuracy — figures, tolls, stats ----

    private DimensionScore scoreNumericAccuracy(String referenceText, String comparisonText) {
        List<NumericClaim> refClaims = NumericClaimExtractor.extract(referenceText);
        List<NumericClaim> targetClaims = NumericClaimExtractor.extract(comparisonText);

        if (refClaims.isEmpty()) {
            return new DimensionScore("Numeric Accuracy", 100.0,
                    List.of("Reference article contains no numeric claims to check."));
        }

        List<String> findings = new ArrayList<>();
        int matched = 0;

        for (NumericClaim refClaim : refClaims) {
            boolean found = targetClaims.stream().anyMatch(t -> withinTolerance(refClaim.normalizedValue(), t.normalizedValue()));
            if (found) {
                matched++;
            } else {
                findings.add("Figure not matched in coverage: \"" + refClaim.rawText()
                        + "\" (from: \"" + truncate(refClaim.context(), 120) + "\")");
            }
        }

        double score = 100.0 * matched / refClaims.size();
        List<String> capped = findings.stream().limit(MAX_FINDINGS_PER_DIMENSION).collect(Collectors.toList());
        if (capped.isEmpty()) {
            capped = List.of("All " + refClaims.size() + " numeric claim(s) matched within tolerance.");
        }
        return new DimensionScore("Numeric Accuracy", round1(score), capped);
    }

    private boolean withinTolerance(double reference, double candidate) {
        if (reference == candidate) {
            return true;
        }
        if (reference == 0) {
            return false;
        }
        return Math.abs(reference - candidate) / Math.abs(reference) <= NUMERIC_RELATIVE_TOLERANCE;
    }

    // ---- Dimension 4: Causal-claim strength — "linked to" vs "causes" overreach ----

    private DimensionScore scoreCausalConsistency(String referenceText, String comparisonText,
                                                    List<String> refSentences, List<String> targetSentences) {
        List<CausalSignal> refSignals = CausalStrengthAnalyzer.extract(referenceText);
        List<CausalSignal> targetSignals = CausalStrengthAnalyzer.extract(comparisonText);

        if (refSignals.isEmpty()) {
            return new DimensionScore("Causal-Claim Strength", 100.0,
                    List.of("Reference article makes no causal claims to check for overreach."));
        }
        if (targetSentences.isEmpty()) {
            return new DimensionScore("Causal-Claim Strength", 100.0, List.of("No comparison text available."));
        }

        Map<String, CausalSignal> targetSignalBySentence = targetSignals.stream()
                .collect(Collectors.toMap(CausalSignal::sentence, s -> s, (a, b) -> a));

        List<String> combined = new ArrayList<>(refSentences);
        combined.addAll(targetSentences);
        TfIdfVectorizer vectorizer = new TfIdfVectorizer(combined);

        List<String> findings = new ArrayList<>();
        int overreachCount = 0;

        for (CausalSignal refSignal : refSignals) {
            int refIndex = refSentences.indexOf(refSignal.sentence());
            if (refIndex < 0) {
                continue;
            }
            double[] refVector = vectorizer.vectorFor(refIndex);

            double bestSim = 0.0;
            String bestTargetSentence = null;
            for (int j = 0; j < targetSentences.size(); j++) {
                double sim = CosineSimilarity.similarity(refVector, vectorizer.vectorFor(refSentences.size() + j));
                if (sim > bestSim) {
                    bestSim = sim;
                    bestTargetSentence = targetSentences.get(j);
                }
            }

            if (bestSim >= CAUSAL_TOPIC_MATCH_THRESHOLD && bestTargetSentence != null
                    && targetSignalBySentence.containsKey(bestTargetSentence)) {
                CausalSignal targetSignal = targetSignalBySentence.get(bestTargetSentence);
                if (targetSignal.strength() > refSignal.strength()) {
                    overreachCount++;
                    findings.add("Possible causal overreach — reference: \"" + refSignal.keyword()
                            + "\" (" + refSignal.strengthLabel() + "), coverage: \"" + targetSignal.keyword()
                            + "\" (" + targetSignal.strengthLabel() + ") in: \""
                            + truncate(targetSignal.sentence(), 140) + "\"");
                }
            }
        }

        double score = 100.0 * (1.0 - ((double) overreachCount / refSignals.size()));
        List<String> capped = findings.stream().limit(MAX_FINDINGS_PER_DIMENSION).collect(Collectors.toList());
        if (capped.isEmpty()) {
            capped = List.of("No causal-claim overreach detected across " + refSignals.size() + " causal statement(s).");
        }
        return new DimensionScore("Causal-Claim Strength", round1(score), capped);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1).trim() + "…";
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
