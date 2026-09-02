package com.veritaspath.config;

import com.veritaspath.dto.ArticleComparisonResult;
import com.veritaspath.dto.ComparisonResponse;
import com.veritaspath.repository.ComparisonRecordRepository;
import com.veritaspath.service.ComparisonHistoryService;
import com.veritaspath.service.ComparisonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Seeds the dashboard with three ready-to-explore demo comparisons on
 * first startup, using the article text under
 * {@code src/main/resources/sample-data}. Skips seeding if the database
 * already has history (e.g. on a restart) so results aren't duplicated.
 */
@Component
public class SampleDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataSeeder.class);

    private final ComparisonService comparisonService;
    private final ComparisonHistoryService historyService;
    private final ComparisonRecordRepository repository;

    public SampleDataSeeder(ComparisonService comparisonService,
                             ComparisonHistoryService historyService,
                             ComparisonRecordRepository repository) {
        this.comparisonService = comparisonService;
        this.historyService = historyService;
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("Comparison history already present ({} records) — skipping sample data seed.", repository.count());
            return;
        }

        seedScenario("bridge-collapse", "Continental Wire",
                "reference-continental-wire.txt",
                List.of("Daily Ledger", "comparison-daily-ledger.txt",
                        "Metro Herald", "comparison-metro-herald.txt"));

        seedScenario("energy-drink-study", "Lindgren Institute Release",
                "reference-institute-release.txt",
                List.of("Viral Health Blog", "comparison-buzzfeed-style.txt"));

        seedScenario("wildfire-evacuation", "County OEM Briefing",
                "reference-county-oem.txt",
                List.of("Northlight Times", "comparison-northlight-times.txt"));

        log.info("Seeded {} demo comparison(s). Open the dashboard to explore them.", repository.count());
    }

    /** {@code outletsAndFiles} alternates outlet name, filename pairs. */
    private void seedScenario(String folder, String referenceOutlet, String referenceFile, List<String> outletsAndFiles) {
        try {
            String referenceText = readSample(folder, referenceFile);

            List<ArticleComparisonResult> results = new java.util.ArrayList<>();
            for (int i = 0; i < outletsAndFiles.size(); i += 2) {
                String outlet = outletsAndFiles.get(i);
                String file = outletsAndFiles.get(i + 1);
                String text = readSample(folder, file);
                results.add(comparisonService.compare(referenceText, outlet, text));
            }

            ComparisonResponse response = new ComparisonResponse(null, referenceOutlet, Instant.now(), results);
            historyService.save(response);
        } catch (IOException e) {
            log.warn("Could not seed sample scenario '{}': {}", folder, e.getMessage());
        }
    }

    private String readSample(String folder, String file) throws IOException {
        ClassPathResource resource = new ClassPathResource("sample-data/" + folder + "/" + file);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
