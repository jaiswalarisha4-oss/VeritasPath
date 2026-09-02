(function () {
  "use strict";

  const API_BASE = "/api";
  const charts = []; // keep Chart.js instances so we can destroy them before re-rendering

  // ---------- navigation ----------

  document.querySelectorAll(".nav-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.querySelectorAll(".nav-btn").forEach((b) => b.classList.remove("active"));
      document.querySelectorAll(".view").forEach((v) => v.classList.remove("active"));
      btn.classList.add("active");
      document.getElementById("view-" + btn.dataset.view).classList.add("active");
      if (btn.dataset.view === "history") {
        loadHistory();
      }
    });
  });

  // ---------- source mode toggles (paste text vs. fetch URL) ----------

  function wireToggle(container) {
    const toggle = container.querySelector(".source-toggle");
    if (!toggle) return;
    toggle.querySelectorAll(".toggle-btn").forEach((btn) => {
      btn.addEventListener("click", () => {
        toggle.querySelectorAll(".toggle-btn").forEach((b) => b.classList.remove("active"));
        btn.classList.add("active");
        const mode = btn.dataset.mode;
        container.querySelectorAll(".mode-text").forEach((el) => el.classList.toggle("hidden", mode !== "text"));
        container.querySelectorAll(".mode-url").forEach((el) => el.classList.toggle("hidden", mode !== "url"));
      });
    });
  }
  wireToggle(document.querySelector(".form-panel"));

  document.getElementById("ref-fetch-btn").addEventListener("click", async () => {
    const url = document.getElementById("ref-url").value.trim();
    if (!url) return;
    await fetchPreviewInto(url, document.getElementById("ref-text"), document.getElementById("ref-fetch-btn"));
    // Switch back to text mode so the fetched text is visible/editable.
    document.querySelector('.form-panel > .source-toggle .toggle-btn[data-mode="text"]').click();
  });

  async function fetchPreviewInto(url, textarea, triggerBtn) {
    const original = triggerBtn.textContent;
    triggerBtn.textContent = "Fetching…";
    triggerBtn.disabled = true;
    try {
      const res = await fetch(`${API_BASE}/articles/preview`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ url }),
      });
      if (!res.ok) throw new Error((await res.json()).error || "Fetch failed");
      const data = await res.json();
      textarea.value = data.text;
    } catch (err) {
      showFormError("Could not fetch that URL: " + err.message);
    } finally {
      triggerBtn.textContent = original;
      triggerBtn.disabled = false;
    }
  }

  // ---------- dynamic comparison rows ----------

  const listEl = document.getElementById("comparison-list");
  const rowTemplate = document.getElementById("comparison-row-template");

  function addComparisonRow(prefill) {
    const node = rowTemplate.content.cloneNode(true);
    const row = node.querySelector(".comparison-row");

    wireToggle(row);
    row.querySelector(".remove-btn").addEventListener("click", () => row.remove());
    row.querySelector(".cmp-fetch-btn").addEventListener("click", async (e) => {
      const url = row.querySelector(".cmp-url").value.trim();
      if (!url) return;
      await fetchPreviewInto(url, row.querySelector(".cmp-text"), e.target);
      row.querySelector('.source-toggle .toggle-btn[data-mode="text"]').click();
    });

    if (prefill) {
      row.querySelector(".cmp-outlet").value = prefill.outlet || "";
      row.querySelector(".cmp-text").value = prefill.text || "";
    }

    listEl.appendChild(row);
  }

  document.getElementById("add-comparison-btn").addEventListener("click", () => addComparisonRow());
  addComparisonRow(); // start with one row

  // ---------- demo chips ----------

  document.querySelectorAll(".chip-btn").forEach((chip) => {
    chip.addEventListener("click", () => {
      const scenario = DEMO_SCENARIOS[chip.dataset.demo];
      if (!scenario) return;

      document.getElementById("ref-outlet").value = scenario.referenceOutlet;
      document.getElementById("ref-text").value = scenario.referenceText;
      document.querySelector('.form-panel > .source-toggle .toggle-btn[data-mode="text"]').click();

      listEl.innerHTML = "";
      scenario.comparisons.forEach((c) => addComparisonRow(c));

      hideFormError();
    });
  });

  // ---------- build request + run ----------

  function articleInputFrom(scopeEl, outletSelector, textSelector, urlSelector) {
    const outlet = scopeEl.querySelector(outletSelector).value.trim();
    const activeMode = scopeEl.querySelector(".source-toggle .toggle-btn.active").dataset.mode;
    const input = { outletName: outlet };
    if (activeMode === "url") {
      input.url = scopeEl.querySelector(urlSelector).value.trim();
    } else {
      input.text = scopeEl.querySelector(textSelector).value.trim();
    }
    return input;
  }

  function buildRequest() {
    const formPanel = document.querySelector(".form-panel");
    const reference = articleInputFrom(formPanel, "#ref-outlet", "#ref-text", "#ref-url");

    const comparisons = Array.from(listEl.querySelectorAll(".comparison-row")).map((row) =>
      articleInputFrom(row, ".cmp-outlet", ".cmp-text", ".cmp-url")
    );

    return { reference, comparisons };
  }

  document.getElementById("run-btn").addEventListener("click", runComparison);

  async function runComparison() {
    hideFormError();
    const payload = buildRequest();

    if (!payload.reference.outletName || (!payload.reference.text && !payload.reference.url)) {
      showFormError("Please provide a reference outlet name and either text or a URL.");
      return;
    }
    if (payload.comparisons.length === 0 || payload.comparisons.some((c) => !c.outletName || (!c.text && !c.url))) {
      showFormError("Every comparison outlet needs a name and either text or a URL.");
      return;
    }

    const btn = document.getElementById("run-btn");
    const original = btn.textContent;
    btn.textContent = "Comparing…";
    btn.disabled = true;

    try {
      const res = await fetch(`${API_BASE}/comparisons`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        const errBody = await res.json().catch(() => ({}));
        throw new Error(errBody.error || `Request failed (${res.status})`);
      }
      const data = await res.json();
      renderResults(data);
    } catch (err) {
      showFormError(err.message);
    } finally {
      btn.textContent = original;
      btn.disabled = false;
    }
  }

  function showFormError(msg) {
    const el = document.getElementById("form-error");
    el.textContent = msg;
    el.classList.remove("hidden");
  }
  function hideFormError() {
    document.getElementById("form-error").classList.add("hidden");
  }

  // ---------- render results ----------

  const resultsContainer = document.getElementById("results-container");
  const resultCardTemplate = document.getElementById("result-card-template");

  function scoreClass(score) {
    if (score >= 80) return "dim-score-good";
    if (score >= 55) return "dim-score-warn";
    return "dim-score-bad";
  }

  // Short axis labels for the radar chart; full names still appear in the findings list below it.
  const SHORT_DIMENSION_LABEL = {
    "Quote Fidelity": "Quotes",
    "Numeric Accuracy": "Numbers",
    "Omission of Content": "Omission",
    "Causal-Claim Strength": "Causal Strength",
  };

  function renderResults(response) {
    charts.forEach((c) => c.destroy());
    charts.length = 0;
    resultsContainer.innerHTML = "";
    document.getElementById("results-hint").textContent =
      `Reference: ${response.referenceOutlet}` + (response.createdAt ? ` · ${new Date(response.createdAt).toLocaleString()}` : "");

    response.results.forEach((result) => {
      const node = resultCardTemplate.content.cloneNode(true);
      const card = node.querySelector(".result-card");

      card.querySelector(".result-outlet").textContent = result.outletName;
      card.querySelector(".score-value").textContent = result.alignmentScore.toFixed(1);
      card.querySelector(".score-value").className = "score-value " + scoreClass(result.alignmentScore);

      const canvas = card.querySelector(".radar-canvas");
      resultsContainer.appendChild(card);

      const chart = new Chart(canvas, {
        type: "radar",
        data: {
          labels: result.dimensions.map((d) => SHORT_DIMENSION_LABEL[d.name] || d.name),
          datasets: [
            {
              label: result.outletName,
              data: result.dimensions.map((d) => d.score),
              backgroundColor: "rgba(30, 58, 138, 0.15)",
              borderColor: "rgba(30, 58, 138, 0.9)",
              pointBackgroundColor: "rgba(30, 58, 138, 0.9)",
            },
          ],
        },
        options: {
          layout: { padding: 24 },
          scales: {
            r: {
              min: 0,
              max: 100,
              ticks: { display: false },
              pointLabels: { font: { size: 11 } },
            },
          },
          plugins: { legend: { display: false } },
        },
      });
      charts.push(chart);

      const findingsEl = card.querySelector(".dimension-findings");
      result.dimensions.forEach((dim) => {
        const block = document.createElement("div");
        block.className = "dim-block";

        const title = document.createElement("div");
        title.className = "dim-title";
        title.innerHTML = `<span>${dim.name}</span><span class="${scoreClass(dim.score)}">${dim.score.toFixed(1)}</span>`;
        block.appendChild(title);

        const list = document.createElement("ul");
        list.className = "dim-findings-list";
        dim.findings.forEach((f) => {
          const li = document.createElement("li");
          li.textContent = f;
          list.appendChild(li);
        });
        block.appendChild(list);

        findingsEl.appendChild(block);
      });
    });
  }

  // ---------- history ----------

  async function loadHistory() {
    const container = document.getElementById("history-container");
    container.innerHTML = "<p class=\"hint\">Loading…</p>";
    try {
      const res = await fetch(`${API_BASE}/comparisons/history`);
      const items = await res.json();
      if (items.length === 0) {
        container.innerHTML = "<p class=\"hint\">No comparisons yet — run one from the Compare tab.</p>";
        return;
      }
      container.innerHTML = "";
      items.forEach((item) => {
        const avg = item.results.reduce((s, r) => s + r.alignmentScore, 0) / item.results.length;
        const el = document.createElement("div");
        el.className = "history-item";
        el.innerHTML = `
          <div>
            <strong>${item.referenceOutlet}</strong>
            <div class="history-meta">${item.results.map((r) => r.outletName).join(", ")} · ${new Date(item.createdAt).toLocaleString()}</div>
          </div>
          <div class="score-badge"><span class="${scoreClass(avg)}">${avg.toFixed(1)}</span><span class="score-max">/100 avg</span></div>
        `;
        el.addEventListener("click", () => {
          document.querySelector('.nav-btn[data-view="compare"]').click();
          renderResults(item);
        });
        container.appendChild(el);
      });
    } catch (err) {
      container.innerHTML = `<p class="error-msg">Could not load history: ${err.message}</p>`;
    }
  }
})();
