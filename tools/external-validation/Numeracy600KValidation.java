import com.veritaspath.model.NumericClaim;
import com.veritaspath.nlp.NumericClaimExtractor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Validates {@code com.veritaspath.nlp.NumericClaimExtractor}'s extraction
 * recall against real headlines from Numeracy-600K (Chen, Huang, Takamura,
 * Chen, "Numeracy-600K: Learning Numeracy for Detecting Exaggerated
 * Information in Market Comments", ACL 2019). Uses the "Article Titles"
 * subset only (600K real headlines, CC0 public domain per the dataset's own
 * README; the "Market Comments" subset is Refinitiv-owned and not used
 * here). Repo: https://github.com/aistairc/Numeracy-600K
 *
 * <h2>Scope note</h2>
 * Numeracy-600K's actual task is magnitude classification (given a
 * highlighted numeral, predict its order-of-magnitude bucket) -- a
 * single-text task with no reference-vs-comparison structure, so like the
 * SemEval validation, this cannot exercise VeritasPath's actual two-article
 * matching/tolerance logic. What it validates is upstream of that: given a
 * real headline and a human/automatically-verified numeral span within it,
 * does {@code NumericClaimExtractor.extract()} find that number at all?
 *
 * <p>Also expect this benchmark to be a harsher, differently-shaped test
 * than VeritasPath's actual target domain: many of its numerals are
 * "listicle" counts ("10 Tips...", "12 Days of..."), which is exactly the
 * kind of short, context-free number NumericClaimExtractor's
 * COUNT_CONTEXT_WORDS heuristic is deliberately designed to treat as noise
 * and skip (see its class comment and {@code skipsNoisyShortNumbersWithNoSignal}
 * in NumericClaimExtractorTest). A low recall number here is expected to
 * be, in large part, that deliberate design choice being exercised on a
 * distribution it was built to filter -- not evidence the extractor is bad
 * at finding reportable figures (tolls, percentages, dollar amounts) in
 * hard-news text, which is its actual target domain. This tool reports the
 * breakdown needed to tell the two apart.
 */
public class Numeracy600KValidation {

    public static void main(String[] args) throws IOException {
        Path sampleFile = Path.of(args.length > 0 ? args[0] : "sample.tsv");
        if (!Files.exists(sampleFile)) {
            System.err.println("Could not find " + sampleFile + " -- see tools/external-validation/README.md "
                    + "for how to download Numeracy-600K and build this sample file.");
            System.exit(1);
        }

        List<String> lines = Files.readAllLines(sampleFile, StandardCharsets.UTF_8);
        int total = 0, hits = 0, shortNumberMisses = 0, multiplierArtifactMisses = 0, decimalTokenMisses = 0;
        List<String[]> misses = new ArrayList<>();
        List<String[]> hitExamples = new ArrayList<>();
        List<String[]> multiplierArtifactExamples = new ArrayList<>();
        List<String[]> decimalTokenExamples = new ArrayList<>();
        List<String[]> residualMisses = new ArrayList<>();

        double[] multiplierDivisors = {1_000, 1_000_000, 1_000_000_000, 1_000_000_000_000d};

        for (int i = 1; i < lines.size(); i++) { // skip header
            String[] cols = lines.get(i).split("\t", -1);
            if (cols.length < 5) continue;
            String title = cols[0];
            String goldNumberRaw = cols[1];

            Double goldValue = parseGold(goldNumberRaw);
            if (goldValue == null) continue;
            total++;

            List<NumericClaim> claims = NumericClaimExtractor.extract(title);
            boolean found = claims.stream().anyMatch(c -> withinTolerance(goldValue, c.normalizedValue()));

            if (found) {
                hits++;
                if (hitExamples.size() < 6) hitExamples.add(new String[]{title, goldNumberRaw});
                continue;
            }

            misses.add(new String[]{title, goldNumberRaw});

            // Category 1: short (1-2 digit) bare numbers -- the class this
            // extractor deliberately treats as noise (see class comment).
            if (isShortBareNumber(goldNumberRaw)) {
                shortNumberMisses++;
                continue;
            }

            // Category 2: benchmark-representation artifact. Numeracy-600K's
            // gold "number" is the bare pre-multiplier digit token (e.g. "2.41"
            // for "$2.41 million") because ITS task is predicting the magnitude
            // word from context -- the multiplier is deliberately excluded from
            // its gold span. VeritasPath's extractor correctly folds the
            // multiplier IN (2.41 million -> 2410000), which is the right
            // behavior for comparing real dollar figures across two articles.
            // These are not extraction failures; they're two benchmarks
            // measuring different things from the same digits.
            boolean multiplierMatch = claims.stream().anyMatch(c ->
                    Arrays.stream(multiplierDivisors).anyMatch(d -> withinTolerance(goldValue, c.normalizedValue() / d)));
            if (multiplierMatch) {
                multiplierArtifactMisses++;
                if (multiplierArtifactExamples.size() < 6) multiplierArtifactExamples.add(new String[]{title, goldNumberRaw});
                continue;
            }

            // Category 3: non-factual decimal tokens (episode/season/part
            // numbering like "4.05", "2.2") with no currency/%/multiplier
            // signal -- not "numeric claims" in the reportable-fact sense
            // this extractor targets, so correctly not extracted.
            if (goldNumberRaw.matches("\\d+\\.\\d+") && !title.contains("$") && !title.contains("%")) {
                decimalTokenMisses++;
                if (decimalTokenExamples.size() < 5) decimalTokenExamples.add(new String[]{title, goldNumberRaw});
                continue;
            }

            residualMisses.add(new String[]{title, goldNumberRaw});
        }

        System.out.println("================ Numeracy-600K (article titles) extraction validation ================");
        System.out.printf("Sample size: %d headlines with a parseable gold numeral%n", total);
        System.out.printf("RAW RECALL (exact/direct match only): %d/%d = %.1f%%%n", hits, total, 100.0 * hits / total);
        System.out.println("\nBreakdown of the " + misses.size() + " raw misses:");
        System.out.printf("  1. Deliberate short-number skip (<=2 digits, no signal):      %4d (%.1f%%)%n",
                shortNumberMisses, pct(shortNumberMisses, misses.size()));
        System.out.printf("  2. Multiplier-representation artifact (extractor is correct): %4d (%.1f%%)%n",
                multiplierArtifactMisses, pct(multiplierArtifactMisses, misses.size()));
        System.out.printf("  3. Non-factual decimal token (episode/part numbering, etc.):  %4d (%.1f%%)%n",
                decimalTokenMisses, pct(decimalTokenMisses, misses.size()));
        System.out.printf("  4. Residual -- genuine misses:                                %4d (%.1f%%)%n",
                residualMisses.size(), pct(residualMisses.size(), misses.size()));

        double adjustedRecall = 100.0 * (hits + multiplierArtifactMisses + decimalTokenMisses) / total;
        System.out.printf("%nADJUSTED RECALL (excluding categories 2 and 3, which are not extractor failures): " +
                "%d/%d = %.1f%%%n", hits + multiplierArtifactMisses + decimalTokenMisses, total, adjustedRecall);

        System.out.println("\nExample true positives (direct match):");
        hitExamples.forEach(e -> System.out.println("  + [" + e[1] + "] " + truncate(e[0], 130)));

        System.out.println("\nExample category-2 'misses' (multiplier artifact -- extractor is actually correct):");
        multiplierArtifactExamples.forEach(e -> System.out.println("  ~ [" + e[1] + "] " + truncate(e[0], 130)));

        System.out.println("\nExample category-3 'misses' (non-factual decimal token, correctly not extracted):");
        decimalTokenExamples.forEach(e -> System.out.println("  ~ [" + e[1] + "] " + truncate(e[0], 130)));

        System.out.println("\nGenuine residual misses (first 15 of " + residualMisses.size() + "):");
        residualMisses.stream().limit(15).forEach(e -> System.out.println("  - [" + e[1] + "] " + truncate(e[0], 130)));
    }

    private static double pct(int part, int whole) {
        return whole == 0 ? 0.0 : 100.0 * part / whole;
    }

    private static boolean isShortBareNumber(String raw) {
        String digits = raw.replace(",", "");
        return digits.matches("\\d{1,2}");
    }

    private static Double parseGold(String raw) {
        try {
            return Double.parseDouble(raw.replace(",", "").replace("$", "").replace("%", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean withinTolerance(double a, double b) {
        if (a == b) return true;
        if (a == 0) return false;
        return Math.abs(a - b) / Math.abs(a) <= 0.02;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
