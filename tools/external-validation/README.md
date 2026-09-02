# External validation against SemEval-2020 Task 11

This tool validates `com.veritaspath.nlp.CausalStrengthAnalyzer` — the
keyword-based causal-language detector underneath VeritasPath's
**Causal-Claim Strength** dimension — against real, professionally
annotated data, instead of only the hand-labeled synthetic pairs in
`src/test/java/com/veritaspath/eval/AccuracyEvaluationTest.java`.

**Read [`docs/EXTERNAL_VALIDATION.md`](../../docs/EXTERNAL_VALIDATION.md)
first** for the full methodology, results, and — most importantly — the
scope caveats on what this does and does not prove. The short version:
recall on real annotated propaganda text is **4.8%** (up from a 1.0%
baseline before this validation drove two rounds of keyword additions),
far below the ~96% this project measures on its own synthetic test data.
That gap is the point of running this: it's honest, external evidence for
why a fine-tuned model (Phase B on the roadmap) is necessary, not just an
assertion.

## Why the corpus isn't in this repository

The Propaganda Techniques Corpus (PTC v2) consists of excerpts from
copyrighted news articles collected from 48 outlets. Its authors released
it for research use through an official channel (Zenodo), not for
open-ended redistribution — so this repository ships the *validation
code*, not the *corpus*. Download it yourself and point the tool at it.

## Reproducing this validation

1. Download the corpus from **https://zenodo.org/records/3952415**
   (the `datasets.tgz`/`datasetsv2.tgz` file — Propaganda Techniques
   Corpus v2, used for SemEval-2020 Task 11).
2. Extract it. You should get a `datasets/` folder containing
   `train-articles/`, `train-task1-SI.labels`, `train-task2-TC.labels`,
   etc. (Only the `train` split has real gold labels; `dev-task-TC-template.out`
   is a masked template used for the original competition leaderboard and
   has no usable labels.)
3. From the repository root, build the project once so the compiled
   classes are on the classpath:
   ```bash
   mvn -q -DskipTests compile
   ```
4. Compile and run this tool, pointing it at the folder that *contains*
   `datasets/` (i.e. the parent of the extracted folder — the tool defaults
   to looking for `./datasets` relative to wherever you run it from):
   ```bash
   cd tools/external-validation
   javac -cp ../../target/classes -d /tmp/semeval-out SemEvalCausalValidation.java
   java -cp "../../target/classes:/tmp/semeval-out" SemEvalCausalValidation /path/to/folder/containing/datasets
   ```
5. It prints: total `Causal_Oversimplification` instances found, recall
   against them, a strength-tier breakdown of what it did detect, a
   sample of true positives and false negatives, and a base-rate check on
   unannotated sentences from the same articles.

## What this validates, and what it doesn't

- **Validates:** whether `CausalStrengthAnalyzer.extract()` — a single-text
  primitive — notices causal-language framing in real sentences that
  professional annotators independently flagged as an oversimplified
  causal claim.
- **Does not validate:** VeritasPath's actual product behavior, which is
  `ComparisonService` flagging "overreach" when a *second* article's
  causal language is *stronger* than a *reference* article's for the
  *same, topically-matched claim*. SemEval-2020 Task 11 has no
  paired-article structure, so there is nothing in this corpus that
  exercises that comparison logic.
- **Says nothing about** the other three VeritasPath dimensions (Quote
  Fidelity, Numeric Accuracy, Omission of Content) — this corpus has no
  ground truth for quotes, figures, or omitted content at all.
