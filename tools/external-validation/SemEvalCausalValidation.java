import com.veritaspath.model.CausalSignal;
import com.veritaspath.nlp.CausalStrengthAnalyzer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Validates {@code com.veritaspath.nlp.CausalStrengthAnalyzer} against real,
 * professionally-annotated data: the Propaganda Techniques Corpus (PTC v2)
 * used for SemEval-2020 Task 11 (Da San Martino, Yu, Barron-Cedeno, Petrov,
 * Nakov, "Fine-Grained Analysis of Propaganda in News Articles", EMNLP-IJCNLP
 * 2019; corpus released via Zenodo: https://zenodo.org/records/3952415).
 *
 * <p><b>This is not run as part of {@code mvn test}</b> because the corpus
 * itself is not redistributed in this repository -- it consists of excerpts
 * from copyrighted news articles from 48 outlets, released by its authors
 * for research use via the official channel above, not for redistribution.
 * See {@code tools/external-validation/README.md} for how to download it
 * and run this tool yourself.
 *
 * <h2>Scope note -- read before trusting any number this prints</h2>
 * SemEval-2020 Task 11 is <em>single-document</em> propaganda technique
 * classification (18 techniques, one of which is "Causal Oversimplification").
 * VeritasPath's Causal-Claim Strength dimension is a <em>two-document</em>
 * task: it compares causal-language strength between a reference article and
 * a second article on the same story, and only flags "overreach" when the
 * second is strictly stronger than the first. This corpus has no
 * paired-article structure, so it cannot validate {@code ComparisonService}'s
 * overreach logic end to end -- only the primitive underneath it can be
 * checked: does {@code CausalStrengthAnalyzer.extract()} notice causal-
 * language framing in sentences that expert annotators independently
 * flagged as making an oversimplified causal claim? That is a recall check
 * on real data. It is <em>not</em> comparable to any published SemEval
 * leaderboard score, because it isn't the same task, and it says nothing
 * about the other three VeritasPath dimensions (Quote Fidelity, Numeric
 * Accuracy, Omission of Content), which this corpus has no ground truth for
 * at all.
 */
public class SemEvalCausalValidation {

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : "datasets");
        Path articlesDir = root.resolve("train-articles");
        Path tcLabels = root.resolve("train-task2-TC.labels");
        Path siLabels = root.resolve("train-task1-SI.labels");

        if (!Files.exists(tcLabels)) {
            System.err.println("Could not find " + tcLabels + " -- see tools/external-validation/README.md "
                    + "for how to download the PTC v2 corpus and point this tool at it.");
            System.exit(1);
        }

        Map<String, String> articleText = new HashMap<>();
        // article id -> spans (any of the 14 technique categories) -- used to find "clean" lines
        Map<String, List<int[]>> anySpanByArticle = new HashMap<>();
        // (articleId#lineIndex) -> line text, deduped, for Causal_Oversimplification hits
        Map<String, String> causalLines = new LinkedHashMap<>();
        int causalInstanceCount = 0;

        for (String line : Files.readAllLines(siLabels, StandardCharsets.UTF_8)) {
            String[] parts = line.split("\t");
            anySpanByArticle.computeIfAbsent(parts[0], k -> new ArrayList<>())
                    .add(new int[]{Integer.parseInt(parts[1]), Integer.parseInt(parts[2])});
        }

        for (String line : Files.readAllLines(tcLabels, StandardCharsets.UTF_8)) {
            String[] parts = line.split("\t");
            String articleId = parts[0];
            String technique = parts[1];
            int start = Integer.parseInt(parts[2]);
            if (!technique.equals("Causal_Oversimplification")) continue;
            causalInstanceCount++;

            String text = articleText.computeIfAbsent(articleId, id -> readArticle(articlesDir, id));
            if (text == null) continue;

            int lineIdx = lineIndexForOffset(text, start);
            String sentence = lineAt(text, lineIdx).trim();
            if (!sentence.isEmpty()) {
                causalLines.putIfAbsent(articleId + "#" + lineIdx, sentence);
            }
        }

        System.out.println("================ SemEval-2020 Task 11 (PTC v2) external validation ================");
        System.out.println("Causal_Oversimplification instances in train-task2-TC.labels: " + causalInstanceCount);
        System.out.println("Unique (article, sentence) pairs after dedup: " + causalLines.size());

        int hits = 0, strong = 0, moderate = 0, weak = 0;
        List<String> misses = new ArrayList<>();
        List<String> exampleHits = new ArrayList<>();

        for (Map.Entry<String, String> e : causalLines.entrySet()) {
            List<CausalSignal> signals = CausalStrengthAnalyzer.extract(e.getValue());
            if (!signals.isEmpty()) {
                hits++;
                int best = signals.stream().mapToInt(CausalSignal::strength).max().orElse(0);
                if (best == CausalSignal.STRONG) strong++;
                else if (best == CausalSignal.MODERATE) moderate++;
                else weak++;
                if (exampleHits.size() < 8) {
                    exampleHits.add("[" + signals.get(0).keyword() + "/" + signals.get(0).strengthLabel() + "] " + e.getValue());
                }
            } else {
                misses.add(e.getValue());
            }
        }

        double recall = 100.0 * hits / causalLines.size();
        System.out.printf("%nRECALL on human-labeled Causal_Oversimplification sentences: %d/%d = %.1f%%%n",
                hits, causalLines.size(), recall);
        System.out.printf("  Of detected hits -- strong: %d, moderate: %d, weak: %d%n", strong, moderate, weak);

        System.out.println("\nExample true positives (detected):");
        exampleHits.forEach(s -> System.out.println("  + " + truncate(s, 160)));

        System.out.println("\nExample false negatives (missed) -- first 10 of " + misses.size() + ":");
        misses.stream().limit(10).forEach(s -> System.out.println("  - " + truncate(s, 160)));

        // Negative control / base rate. NOT a precision metric for propaganda
        // detection -- a "clean" news sentence can legitimately contain real,
        // non-propagandistic causal language ("Smoking causes cancer" is a
        // true claim), and CausalStrengthAnalyzer's actual job in VeritasPath
        // is comparing causal strength between two articles' matched claims,
        // not classifying single sentences as propaganda. Reported for
        // honesty about the detector's general trigger rate, not as a score.
        List<String> cleanLines = new ArrayList<>();
        List<String> articleIds = new ArrayList<>(anySpanByArticle.keySet());
        Collections.sort(articleIds);
        for (String id : articleIds) {
            String text = articleText.computeIfAbsent(id, aid -> readArticle(articlesDir, aid));
            if (text == null) continue;
            List<int[]> spans = anySpanByArticle.get(id);
            String[] lines = text.split("\n", -1);
            int offset = 0;
            for (String rawLine : lines) {
                int lineStart = offset, lineEnd = offset + rawLine.length();
                offset = lineEnd + 1;
                String trimmed = rawLine.trim();
                if (trimmed.length() < 20) continue; // skip title/blank/very short lines
                boolean overlaps = spans.stream().anyMatch(sp -> sp[0] < lineEnd && sp[1] > lineStart);
                if (!overlaps) cleanLines.add(trimmed);
            }
        }
        Collections.shuffle(cleanLines, new Random(42));
        List<String> cleanSample = cleanLines.subList(0, Math.min(500, cleanLines.size()));
        long cleanFlagged = cleanSample.stream().filter(s -> !CausalStrengthAnalyzer.extract(s).isEmpty()).count();

        System.out.printf("%nBase rate on %d random *unannotated* sentences (no propaganda label of any kind): " +
                        "%d flagged (%.1f%%) -- see note above; this is a trigger-rate check, not a false-positive rate.%n",
                cleanSample.size(), cleanFlagged, 100.0 * cleanFlagged / cleanSample.size());
    }

    private static String readArticle(Path dir, String articleId) {
        try {
            return Files.readString(dir.resolve("article" + articleId + ".txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private static int lineIndexForOffset(String text, int offset) {
        int idx = 0, count = 0;
        while (true) {
            int next = text.indexOf('\n', idx);
            if (next == -1 || next >= offset) return count;
            idx = next + 1;
            count++;
        }
    }

    private static String lineAt(String text, int lineIndex) {
        String[] lines = text.split("\n", -1);
        return lineIndex < lines.length ? lines[lineIndex] : "";
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
