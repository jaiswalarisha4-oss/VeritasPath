# External validation

VeritasPath's internal accuracy numbers (`AccuracyEvaluationTest`) are agreement with
hand-labeled test data written for this project — a real, documented limitation
(see `docs/ARCHITECTURE.md`). This folder validates all four scoring dimensions'
underlying primitives against real, independently published, human- or
professionally-annotated data instead.

**Read [`docs/EXTERNAL_VALIDATION.md`](../../docs/EXTERNAL_VALIDATION.md) first**
for the full methodology, exact numbers, and — most importantly — the scope
caveats for each one. None of these benchmarks perform VeritasPath's actual
task (comparing a reference article to a second article across four named
dimensions); no such benchmark exists publicly. Each one validates the
narrower primitive a dimension is built on, against real data nobody on this
project wrote or curated. Headline results:

| Dimension | Benchmark | What's validated | Result |
|---|---|---|---|
| Causal-Claim Strength | SemEval-2020 Task 11 (PTC v2) | Does the causal-language detector notice real, human-labeled propaganda? | **4.8% recall** (up from 1.0%) |
| Quote Fidelity | STS Benchmark (SemEval-2017 Task 1) | Does Levenshtein similarity track human judgment of "same statement, different wording"? | **Pearson r = 0.41**; 19.2% of true paraphrases misclassified as "missing" |
| Omission of Content | STS Benchmark (same data) | Does TF-IDF cosine similarity track human similarity judgment? | **Pearson r = 0.66**; 97.9% of true paraphrases correctly clear the omission threshold |
| Numeric Accuracy | Numeracy-600K (ACL 2019) | Does the extractor find real numerals in real headlines? | **37.5%** adjusted recall (raw 35.7%; most of the gap is benchmark-representation differences, not bugs — see doc) |

None of the datasets are committed to this repository — they're either
copyrighted news text released for research use only (PTC v2), or simply
much cleaner to fetch fresh than to vendor. Each tool below documents exactly
how to get its data and reproduce the numbers above.

## 1. Causal-Claim Strength — `SemEvalCausalValidation.java`

Validates `CausalStrengthAnalyzer` against SemEval-2020 Task 11's Propaganda
Techniques Corpus (PTC v2).

1. Download from **https://zenodo.org/records/3952415** (the
   `datasets.tgz`/`datasetsv2.tgz` file).
2. Extract it — you should get a `datasets/` folder with `train-articles/`,
   `train-task1-SI.labels`, `train-task2-TC.labels`, etc.
3. From the repo root: `mvn -q -DskipTests compile`
4. ```bash
   cd tools/external-validation
   javac -cp ../../target/classes -d /tmp/out SemEvalCausalValidation.java
   java -cp "../../target/classes:/tmp/out" SemEvalCausalValidation /path/to/folder/containing/datasets
   ```

## 2. Numeric Accuracy — `Numeracy600KValidation.java`

Validates `NumericClaimExtractor` against Numeracy-600K's article-titles
subset (Chen, Huang, Takamura, Chen, ACL 2019; CC0 public domain, unlike the
paper's other "market comments" subset, which is Refinitiv-owned).

1. Download `Numeracy_600K_article_title.zip` from
   **https://github.com/aistairc/Numeracy-600K** and unzip it — you'll get
   `Numeracy_600K_article_title.json` (~600K headlines with a gold numeral
   span and offset each).
2. Build a sample TSV (the full file is ~100MB; a random sample of a few
   thousand rows is statistically sufficient and much faster to run):
   ```bash
   python3 - <<'EOF'
   import json, random, csv
   with open("Numeracy_600K_article_title.json", encoding="utf-8") as f:
       data = json.load(f)
   random.seed(42)
   sample = random.sample(data, 4000)
   with open("sample.tsv", "w", encoding="utf-8", newline="") as out:
       w = csv.writer(out, delimiter="\t")
       w.writerow(["title", "number", "offset", "length", "magnitude"])
       for row in sample:
           w.writerow([row["title"].replace("\t", " "), row["number"], row["offset"], row["length"], row["magnitude"]])
   EOF
   ```
3. ```bash
   javac -cp ../../target/classes -d /tmp/out Numeracy600KValidation.java
   java -cp "../../target/classes:/tmp/out" Numeracy600KValidation sample.tsv
   ```

## 3. Quote Fidelity & Omission of Content — `StsbSimilarityValidation.java`

Validates both the Levenshtein-similarity primitive (Quote Fidelity) and the
TF-IDF cosine-similarity primitive (Omission of Content) against the STS
Benchmark — Cer, Diab, Agirre, Lopez-Gazpio, Specia, "SemEval-2017 Task 1:
Semantic Textual Similarity Multilingual and Crosslingual Focused
Evaluation" — one of the standard, widely-cited human-annotated
sentence-similarity benchmarks in NLP.

1. Download the English test split (the original STS Benchmark data,
   packaged alongside machine-translated variants for other languages):
   ```bash
   curl -O https://raw.githubusercontent.com/PhilipMay/stsb-multi-mt/main/data/stsb-en-test.csv
   ```
2. Convert to a clean tab-separated file (the source CSV has quoted fields
   with embedded commas):
   ```bash
   python3 - <<'EOF'
   import csv
   with open("stsb-en-test.csv", newline="", encoding="utf-8") as f:
       rows = list(csv.reader(f))
   with open("stsb_clean.tsv", "w", encoding="utf-8", newline="") as out:
       for r in rows:
           if len(r) == 3:
               out.write(f"{r[0].strip()}\t{r[1].strip()}\t{r[2]}\n")
   EOF
   ```
3. ```bash
   javac -cp ../../target/classes -d /tmp/out StsbSimilarityValidation.java
   java -cp "../../target/classes:/tmp/out" StsbSimilarityValidation stsb_clean.tsv
   ```
