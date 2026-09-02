package com.veritaspath.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritaspath.dto.ComparisonResponse;
import com.veritaspath.entity.ComparisonRecord;
import com.veritaspath.repository.ComparisonRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Persists finished comparison runs and re-hydrates them for the dashboard's history view. */
@Service
public class ComparisonHistoryService {

    private final ComparisonRecordRepository repository;
    private final ObjectMapper objectMapper;

    public ComparisonHistoryService(ComparisonRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public ComparisonResponse save(ComparisonResponse response) {
        try {
            double average = response.results().stream()
                    .mapToDouble(r -> r.alignmentScore())
                    .average()
                    .orElse(0.0);

            String json = objectMapper.writeValueAsString(response);
            ComparisonRecord record = new ComparisonRecord(response.referenceOutlet(), average, json);
            ComparisonRecord saved = repository.save(record);

            return new ComparisonResponse(saved.getId(), response.referenceOutlet(), saved.getCreatedAt(), response.results());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist comparison result", e);
        }
    }

    public List<ComparisonResponse> recentHistory() {
        return repository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private ComparisonResponse toResponse(ComparisonRecord record) {
        try {
            ComparisonResponse parsed = objectMapper.readValue(record.getResultJson(), ComparisonResponse.class);
            return new ComparisonResponse(record.getId(), record.getReferenceOutlet(), record.getCreatedAt(), parsed.results());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read stored comparison result " + record.getId(), e);
        }
    }
}
