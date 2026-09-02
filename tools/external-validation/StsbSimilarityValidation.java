import com.veritaspath.nlp.CosineSimilarity;
import com.veritaspath.nlp.TfIdfVectorizer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Validates the two similarity primitives shared by VeritasPath's Quote
 * Fidelity and Omission of Content dimensions against the STS Benchmark
 * (Cer, Diab, Agirre, Lopez-Gazpio, Specia, "SemEval-2017 Task 1: Semantic
 * Textual Similarity Multilingual and Crosslingual Focused Evaluation",
 * SemEval-2017) -- one of the most widely used, peer-reviewed, human-
 * annotated sentence-similarity benchmarks in NLP. English test split
 * (1379 pairs, each scored 0-5 by human annotators) via the redistribution
 * at https://github.com/PhilipMay/stsb-multi-mt (translations of the
 * official STS Benchmark; the English split is the original data).
 *
 * <h2>What this validates, and what it doesn't</h2>
 * VeritasPath does not do general sentence-similarity scoring as a
 * standalone task. It uses two different, narrower similarity primitives
 * as building blocks:
 * <ul>
 *   <li>{@code CosineSimilarity.levenshteinSimilarity} -- character-level
 *   edit distance, used by Quote Fidelity to decide whether a quote in the
 *   comparison article is the same wording as the reference quote (intact
 *   &ge;0.85, altered/paraphrased 0.5-0.85, missing &lt;0.5).</li>
 *   <li>TF-IDF cosine similarity (via {@code TfIdfVectorizer} +
 *   {@code CosineSimilarity.similarity}) -- bag-of-words overlap, used by
 *   Omission of Content to decide whether a reference sentence has any
 *   matching counterpart anywhere in the comparison article (threshold
 *   0.12).</li>
 * </ul>
 * STS Benchmark's task -- "how similar in meaning are these two sentences,
 * 0-5?" -- is not identical to either of those (it's not about quotes
 * specifically, and it's not about find-a-match-anywhere-in-a-document),
 * but the underlying question is the same one both primitives are built
 * on: does a string/lexical similarity score track human judgment of
 * "same statement, different wording"? Correlation against STS-B's gold
 * scores is a standard, direct way to check that, and it's exactly the
 * evaluation protocol the STS literature itself uses.
 */
public class StsbSimilarityValidation {

    public static void main(String[] args) throws IOException {
        Path file = Path.of(args.length > 0 ? args[0] : "stsb_clean.tsv");
        if (!Files.exists(file)) {
            System.err.println("Could not find " + file + " -- see tools/external-validation/README.md.");
            System.exit(1);
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<Double> gold = new ArrayList<>();
        List<Double> levenshtein = new ArrayList<>();
        List<Double> tfidfCosine = new ArrayList<>();
        List<String[]> rows = new ArrayList<>();

        for (String line : lines) {
            String[] parts = line.split("\t", -1);
            if (parts.length != 3) continue;
            String s1 = parts[0], s2 = parts[1];
            double goldScore;
            try {
                goldScore = Double.parseDouble(parts[2]);
            } catch (NumberFormatException e) {
                continue;
            }

            double lev = CosineSimilarity.levenshteinSimilarity(s1, s2);

            TfIdfVectorizer vec = new TfIdfVectorizer(List.of(s1, s2));
            double cos = CosineSimilarity.similarity(vec.vectorFor(0), vec.vectorFor(1));

            gold.add(goldScore);
            levenshtein.add(lev);
            tfidfCosine.add(cos);
            rows.add(new String[]{s1, s2, String.valueOf(goldScore), String.valueOf(lev), String.valueOf(cos)});
        }

        System.out.println("================ STS Benchmark similarity-primitive validation ================");
        System.out.println("Pairs evaluated: " + gold.size());

        double[] goldArr = toArray(gold);
        System.out.printf("%nQuote Fidelity primitive (Levenshtein similarity) vs human gold score:%n");
        System.out.printf("  Pearson r  = %.3f%n", pearson(goldArr, toArray(levenshtein)));
        System.out.printf("  Spearman r = %.3f%n", spearman(goldArr, toArray(levenshtein)));

        System.out.printf("%nOmission-of-Content primitive (TF-IDF cosine) vs human gold score:%n");
        System.out.printf("  Pearson r  = %.3f%n", pearson(goldArr, toArray(tfidfCosine)));
        System.out.printf("  Spearman r = %.3f%n", spearman(goldArr, toArray(tfidfCosine)));

        // Threshold behavior on near-equivalent pairs (gold >= 4.0 -- "mostly
        // or completely equivalent" per the STS annotation guidelines): this
        // is what a real paraphrased-but-faithful quote or a paraphrased-but-
        // covered reference sentence looks like. If VeritasPath's thresholds
        // were tuned for this, most of these should NOT be flagged as
        // "missing" (Quote Fidelity) or "omitted" (Omission).
        List<double[]> highGold = new ArrayList<>();
        for (int i = 0; i < gold.size(); i++) {
            if (gold.get(i) >= 4.0) highGold.add(new double[]{levenshtein.get(i), tfidfCosine.get(i)});
        }
        long levIntact = highGold.stream().filter(r -> r[0] >= 0.85).count();
        long levAltered = highGold.stream().filter(r -> r[0] >= 0.5 && r[0] < 0.85).count();
        long levMissing = highGold.stream().filter(r -> r[0] < 0.5).count();
        long omissionCovered = highGold.stream().filter(r -> r[1] >= 0.12).count();
        long omissionFlagged = highGold.stream().filter(r -> r[1] < 0.12).count();

        System.out.printf("%nOn the %d pairs humans rated >=4.0/5 (mostly/completely equivalent meaning):%n", highGold.size());
        System.out.printf("  Quote Fidelity tiers if these were quotes -- intact: %d (%.1f%%), altered: %d (%.1f%%), missing: %d (%.1f%%)%n",
                levIntact, pct(levIntact, highGold.size()), levAltered, pct(levAltered, highGold.size()), levMissing, pct(levMissing, highGold.size()));
        System.out.printf("  Omission threshold if these were reference/coverage sentences -- covered: %d (%.1f%%), incorrectly flagged omitted: %d (%.1f%%)%n",
                omissionCovered, pct(omissionCovered, highGold.size()), omissionFlagged, pct(omissionFlagged, highGold.size()));

        System.out.println("\nExample high-meaning-similarity pairs where Levenshtein would call the quote 'missing' (score < 0.5):");
        rows.stream()
                .filter(r -> Double.parseDouble(r[2]) >= 4.0 && Double.parseDouble(r[3]) < 0.5)
                .limit(6)
                .forEach(r -> System.out.printf("  gold=%.1f lev=%.2f cos=%.2f | \"%s\" vs \"%s\"%n",
                        Double.parseDouble(r[2]), Double.parseDouble(r[3]), Double.parseDouble(r[4]), r[0], r[1]));

        // Is there headroom to lower the 0.5 "missing" cutoff without creating
        // false "altered" classifications for genuinely unrelated pairs?
        List<Double> lowGoldLev = new ArrayList<>();
        for (int i = 0; i < gold.size(); i++) if (gold.get(i) <= 1.0) lowGoldLev.add(levenshtein.get(i));
        Collections.sort(lowGoldLev);
        double p95LowGold = lowGoldLev.isEmpty() ? 0 : lowGoldLev.get((int) (lowGoldLev.size() * 0.95));
        System.out.printf("%nFor calibration: 95th percentile Levenshtein similarity among the %d pairs " +
                        "humans rated <=1.0/5 (essentially unrelated) is %.2f -- i.e. lowering the 0.5 'missing' " +
                        "cutoff toward there would catch more true paraphrases as 'altered' instead of 'missing' " +
                        "without meaningfully risking genuinely unrelated quotes being called 'altered'.%n",
                lowGoldLev.size(), p95LowGold);
    }

    private static double[] toArray(List<Double> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private static double pct(long part, int whole) {
        return whole == 0 ? 0.0 : 100.0 * part / whole;
    }

    private static double pearson(double[] x, double[] y) {
        int n = x.length;
        double meanX = Arrays.stream(x).average().orElse(0), meanY = Arrays.stream(y).average().orElse(0);
        double num = 0, denomX = 0, denomY = 0;
        for (int i = 0; i < n; i++) {
            double dx = x[i] - meanX, dy = y[i] - meanY;
            num += dx * dy;
            denomX += dx * dx;
            denomY += dy * dy;
        }
        return num / Math.sqrt(denomX * denomY);
    }

    private static double spearman(double[] x, double[] y) {
        return pearson(rank(x), rank(y));
    }

    private static double[] rank(double[] values) {
        Integer[] idx = new Integer[values.length];
        for (int i = 0; i < idx.length; i++) idx[i] = i;
        Arrays.sort(idx, Comparator.comparingDouble(i -> values[i]));
        double[] ranks = new double[values.length];
        int i = 0;
        while (i < idx.length) {
            int j = i;
            while (j + 1 < idx.length && values[idx[j + 1]] == values[idx[i]]) j++;
            double avgRank = (i + j) / 2.0 + 1;
            for (int k = i; k <= j; k++) ranks[idx[k]] = avgRank;
            i = j + 1;
        }
        return ranks;
    }
}
