package com.veritaspath.repository;

import com.veritaspath.entity.ComparisonRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComparisonRecordRepository extends JpaRepository<ComparisonRecord, Long> {

    List<ComparisonRecord> findTop20ByOrderByCreatedAtDesc();
}
