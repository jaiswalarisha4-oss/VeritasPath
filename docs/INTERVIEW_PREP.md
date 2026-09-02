# Interview prep: likely questions and honest answers

This project is designed to be defended, not just demoed. These are the questions most likely to come up, with the answers this codebase actually supports (file/line references so you can pull the code up live).

---

### "Walk me through what happens when I submit a comparison."

The dashboard POSTs a `reference` article and a list of `comparisons` to `POST /api/comparisons` (`ComparisonController.compare`). For each comparison article, `ComparisonService.compare` runs the reference text and that article's text through four independent scoring methods — one per dimension — and combines them into a weighted composite. The whole result is persisted to H2 and returned as JSON, which the frontend renders as a radar chart plus a findings list per dimension. See `docs/ARCHITECTURE.md` for the sequence diagram.

### "Why did you implement TF-IDF yourself instead of using a library?"

Two reasons, in order of how I'd actually explain it: first, this project's whole pitch is *explainable* claim-level scoring — if the similarity number came out of a library I couldn't fully account for, I couldn't defend *why* two sentences scored 0.6 similar instead of 0.8. Writing it myself (`TfIdfVectorizer`, `CosineSimilarity`) means I can point at the exact formula. Second, it removes an external dependency that needs a model download at build time — a library like OpenNLP needs `.bin` model files fetched separately, which is one more thing that can break in a fresh clone or CI run. TF-IDF is genuinely simple enough (~150 lines) that "write it yourself" beat "pull a dependency" on both counts here.

### "This is bag-of-words — doesn't that miss a lot?"

Yes, and I say so directly in `docs/ARCHITECTURE.md`'s limitations section rather than hiding it. TF-IDF has no concept of word order or negation: "the bridge collapsed" and "the bridge did not collapse" share almost all their significant words and would score as similar. That's exactly why the project's own roadmap (mirroring the source idea's phased plan) puts a fine-tuned transformer as the next tier — Phase A (this build) establishes a validation floor with something fully inspectable; Phase B would fix the negation/paraphrase blind spot at the cost of losing that inspectability.

### "Show me a case where your system is wrong."

The causal-overreach detector doesn't understand negation. Feed it *"Researchers say we cannot say the beverage causes headaches"* and it flags "causes" as a **strong** causal claim — because it's a keyword matcher, not a parser, and it doesn't see the negation scope in front of the keyword. You can reproduce this with the built-in "Health study" demo: the reference article itself contains the word "causes" inside a sentence that's explicitly *disclaiming* causation, and the current implementation can't tell the difference. I'd fix this with a dependency parse or a small classifier trained on causal-claim polarity, not more keywords — more keywords just trades false negatives for false positives.

### "How do you decide the weights (30/30/25/15)?"

Judgment call, documented as a named constant block in `ComparisonService` rather than buried in the math, specifically so it's easy to point at and defend or change. Quotes and numbers get the highest weight (30% each) because they're objectively checkable — a quote either matches or it doesn't, a number either matches within tolerance or it doesn't. Omission gets 25% because it's a real signal but has more false-positive risk (short reference sentences are filtered out at 40 characters to reduce noise, but it's still similarity-threshold-based). Causal strength gets the lowest weight (15%) *because* I know its negation blind spot makes it the least precise of the four — the weighting reflects my own confidence in each dimension's precision.

### "Why Java / Spring Boot for an NLP project instead of Python?"

Two honest reasons: it's the language this was scoped in for the assignment, and Spring Boot's combination of embedded Tomcat + Spring Data JPA + validation meant I could get a working REST API, persistence layer, and static frontend serving from one `mvn spring-boot:run` with no separate deployment story. Python has stronger off-the-shelf NLP tooling (spaCy, scikit-learn's TfidfVectorizer), but since the scoring logic here is hand-implemented rather than library-driven anyway, that advantage mattered less than it would for a model-heavy project.

### "How did you test the NLP logic, given there's no ground-truth dataset?"

Two layers. `nlp/*Test` unit-tests each primitive (TF-IDF, quote extraction, numeric extraction, causal detection) against small, hand-constructed sentences where I know the correct answer by inspection — e.g. "identical sentences should have cosine similarity 1.0," "a scare quote with no space shouldn't be extracted as a real quote." `ComparisonServiceTest` then tests the *dimensions* end-to-end with scenario pairs: an identical article should score near-100 on everything; changing only a number should drop only the Numeric Accuracy score, not the others. That isolation — one deliberate change, one dimension should move — is what makes the tests meaningful rather than just "does it run."

### "What would you build next if you had more time?"

In the order the original project brief phased it, and the order that matches this codebase's own roadmap section: a fine-tuned transformer per dimension (fixes the negation/paraphrase gap TF-IDF can't), then a zero-shot LLM-judge tier as a third comparable baseline, then — as a genuine stretch goal, not core — cryptographically anchoring the reference document (via C2PA tooling, not homebrew crypto) so "reference" means verified rather than just "whatever I pasted first," and cascade attribution across more than two articles to find *where in a citation chain* a distortion entered, not just that it exists.

### "Why didn't you use a real NLP library's number/date extraction?"

Same reasoning as TF-IDF: explainability and zero external model dependencies. The tradeoff is real and I'd say so directly — my numeric extractor's word-number list only covers one through ninety and a curated set of "count context" words (`people`, `killed`, `percent`, ...); it will miss "a couple hundred" or unusual phrasing a proper library might catch. That's a documented scope limitation, not something I'd claim is complete.

### "What's the single most defensible design decision in this project?"

Scoring on four *named, separately visible* dimensions instead of one opaque number. A single "78% similar" score tells a reader nothing about *what* changed. Showing "Quote Fidelity: 26.6 — this exact quote is missing" next to "Numeric Accuracy: 100 — every figure matched" tells them precisely where to look. That's also why each `DimensionScore` carries its own `findings` list pointing at the specific sentence, quote, or number that produced the score — the goal throughout was that every number on the dashboard should be traceable to a specific piece of text, not just asserted.
