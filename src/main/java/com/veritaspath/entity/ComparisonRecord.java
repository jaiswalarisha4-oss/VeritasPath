package com.veritaspath.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Persisted history of a comparison run. The full scored result is stored
 * as a JSON blob ({@code resultJson}) so the dashboard's history view can
 * re-render past comparisons without recomputing them; the flat columns
 * exist so the history list can be queried/sorted cheaply.
 */
@Entity
@Table(name = "comparison_record")
public class ComparisonRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String referenceOutlet;

    @Column(nullable = false)
    private double averageAlignmentScore;

    @Column(nullable = false)
    private Instant createdAt;

    @Lob
    @Column(nullable = false)
    private String resultJson;

    protected ComparisonRecord() {
        // JPA
    }

    public ComparisonRecord(String referenceOutlet, double averageAlignmentScore, String resultJson) {
        this.referenceOutlet = referenceOutlet;
        this.averageAlignmentScore = averageAlignmentScore;
        this.resultJson = resultJson;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getReferenceOutlet() {
        return referenceOutlet;
    }

    public double getAverageAlignmentScore() {
        return averageAlignmentScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getResultJson() {
        return resultJson;
    }
}
