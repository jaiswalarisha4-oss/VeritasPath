# External validation against SemEval-2020 Task 11

Every other accuracy number in this project (`README.md`'s Accuracy
section, `AccuracyEvaluationTest`) is agreement with hand-labeled test
data written by hand for this project. That is documented as a real
limitation in `ARCHITECTURE.md` — no external, independently-annotated
benchmark had been checked against. This document closes part of that
gap: a real run against professionally-annotated data, with the honest
result, including where it's bad.

## The dataset

**Propaganda Techniques Corpus (PTC) v2**, released for SemEval-2020 Task
11 ("Detection of Propaganda Techniques in News Articles"):

> G. Da San Martino, S. Yu, A. Barrón-Cedeño, R. Petrov and P. Nakov,
> "Fine-Grained Analysis of Propaganda in News Articles", EMNLP-IJCNLP
> 2019.

371 training articles (from 48 news outlets) manually annotated by six
professional annotators for 18 fine-grained propaganda techniques (merged
to 14 for the classification subtask), each labeled with character
offsets. Available at **https://zenodo.org/records/3952415**. See
`tools/external-validation/README.md` for how to download it and
reproduce everything below — the corpus itself is not included in this
repository (see that file for why).

## Why this only tests one of VeritasPath's four dimensions

SemEval-2020 Task 11 is **single-document** propaganda technique
classification. VeritasPath's four dimensions are all **two-document**
comparisons (reference article vs. a second article on the same story).
Only one of the 14 technique categories has any conceptual overlap with
VeritasPath at all: **Causal Oversimplification**, which maps to the
**Causal-Claim Strength** dimension — specifically, to the primitive
underneath it, `CausalStrengthAnalyzer.extract()`.

There is no dataset used here (or found) with a paired-article structure,
so this cannot validate `ComparisonService`'s actual overreach logic
(comparing strength between a matched reference/coverage sentence pair).
It validates a narrower, still-meaningful question: **given a sentence a
professional annotator flagged as making an oversimplified causal claim,
does VeritasPath's keyword-based detector even notice causal framing in
it at all?** Quote Fidelity, Numeric Accuracy, and Omission of Content
have no ground truth in this corpus whatsoever and are not addressed by
this document.

## Method

1. Parsed `train-task2-TC.labels` for every `Causal_Oversimplification`
   instance: **209 instances across 103 of the 371 training articles**
   (the dev split's labels are masked placeholders in the public release,
   used for the original competition leaderboard — unusable here).
2. Mapped each instance's character offset to its containing line, using
   the corpus's own "one sentence per line" convention (stated in the
   corpus README). Deduplicated to **208 unique (article, sentence)**
   pairs (one pair had two overlapping instances on the same line).
3. Ran `CausalStrengthAnalyzer.extract()` on each sentence. A "hit" is any
   non-empty result — regardless of the strength tier it assigned.
4. As a base-rate check (not a precision metric — see caveat below), ran
   the same detector against 500 random sentences from the same articles
   that carry **no** propaganda label of any of the 14 technique types.

## Results

**Baseline** (the keyword list as shipped in the first version of this
project, before this validation): **2/208 = 1.0% recall.**

That number drove two rounds of principled keyword additions to
`CausalStrengthAnalyzer` — `responsible for`, `blamed for` / `to blame
for`, `gave rise to` / `giving rise to` (strong tier), and `led to`,
`enabled`, `fueled`, `sparked`, `prompted`, `set off` (moderate tier),
all found by reading the actual false negatives. **Deliberately not
added:** `because of` and `due to` — they appeared in a handful of missed
instances too, but are common enough in ordinary explanatory prose
("delayed because of weather") that adding them risked a much higher
false-positive rate for a small recall gain; that trade was rejected.

**After the fix:**

```
RECALL on human-labeled Causal_Oversimplification sentences: 10/208 = 4.8%
  Of detected hits -- strong: 4, moderate: 6, weak: 0
Base rate on 500 random unannotated sentences: 5 flagged (1.0%)
```

Base rate on unannotated sentences stayed low (1.0%, up from 0.8%
pre-fix) — the additions were targeted, not a blanket loosening.

### Example true positives (detected)

- *"...set off a firestorm of outrage..."* — `set off` (moderate)
- *"...Jews were 'responsible for all of this filth...'"* — `responsible for` (strong)
- *"...a misunderstanding caused by uninformed people..."* — `caused by` (strong)
- *"...Obama...removed the remaining U.S. combat troops from Iraq, giving rise to ISIS's re-emergence..."* — `giving rise to` (strong)
- *"The outbreak has been fueled by performing the ancient practice..."* — `fueled` (moderate)

### Example false negatives (missed)

- *"Garcia Zarate's deportation and criminal history made him an effective target for immigration hardliners, who argued that Steinle would still be alive were it not for..."* — a counterfactual conditional, no causal connective word at all
- *"They are a perfect match for each other."* — implicit insinuation, zero causal vocabulary
- *"Trump is in the driver's seat because the CIA cannot afford to permit..."* — uses "because," which was deliberately excluded (see above)
- *"There could be only one answer: communists."* — rhetorical false-dichotomy framing, not a lexical causal claim at all

## What this actually shows

The false negatives above are the real finding, not the recall number
itself. Most of SemEval's `Causal_Oversimplification` instances are
**not** expressed through explicit causal connective words like "causes"
or "leads to" — the pattern this project's own synthetic test data
happens to use, because that data was modeled on formal
scientific/press-release hedging language ("the study found an
*association*..."). Real-world propaganda expresses oversimplified
causation through counterfactuals ("would still be alive were it not
for..."), implicit blame attribution, rhetorical insinuation, and
false-dichotomy framing — none of which a keyword matcher can see, almost
by definition, regardless of how large the keyword list gets. A
back-of-envelope check of the 198 remaining misses found at most a
double-digit number containing any plausible additional keyword at all
(`because of` appeared in 7; nothing else appeared more than once) — so
this is a ceiling being hit, not a list that's merely incomplete.

This is exactly the argument `docs/ARCHITECTURE.md`'s roadmap already
made for why Phase B (a fine-tuned transformer) is the necessary next
step rather than "a bigger keyword list" — this document is that claim
backed by a number instead of an assertion: **~95% of real,
professionally-labeled causal-oversimplification instances get past this
project's classical-NLP detector.** That is the honest state of the
Causal-Claim Strength dimension against real adversarial text, as
distinct from its ~100% score on this project's own synthetic test
cases — and the gap between those two numbers is itself the most useful
single fact this validation produced.
