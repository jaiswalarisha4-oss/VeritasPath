package com.veritaspath.controller;

import com.veritaspath.dto.ArticleComparisonResult;
import com.veritaspath.dto.ArticleInput;
import com.veritaspath.dto.ComparisonRequest;
import com.veritaspath.dto.ComparisonResponse;
import com.veritaspath.service.ComparisonHistoryService;
import com.veritaspath.service.ComparisonService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** Runs a reference-vs-outlet(s) comparison and returns/persists the scored result. */
@RestController
@RequestMapping("/api/comparisons")
public class ComparisonController {

    private final ComparisonService comparisonService;
    private final ComparisonHistoryService historyService;

    public ComparisonController(ComparisonService comparisonService, ComparisonHistoryService historyService) {
        this.comparisonService = comparisonService;
        this.historyService = historyService;
    }

    @PostMapping
    public ComparisonResponse compare(@Valid @RequestBody ComparisonRequest request) {
        String referenceText = comparisonService.resolveText(request.getReference());

        List<ArticleComparisonResult> results = request.getComparisons().stream()
                .map((ArticleInput input) -> comparisonService.compare(referenceText, input))
                .toList();

        ComparisonResponse response = new ComparisonResponse(
                null, request.getReference().getOutletName(), Instant.now(), results);

        return historyService.save(response);
    }

    @GetMapping("/history")
    public List<ComparisonResponse> history() {
        return historyService.recentHistory();
    }
}
