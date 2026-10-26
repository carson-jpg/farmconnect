package com.farmconnect.repository;

import com.farmconnect.model.CropCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface CropCaseRepository extends JpaRepository<CropCase, Long> {
    List<CropCase> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);
    /** Newest 500 only, so a very long history never loads at once. */
    List<CropCase> findTop500ByOrderByCreatedAtDesc();
    long countByFarmerIdAndCreatedAtAfter(Long farmerId, Instant since);
    long countByStatus(String status);
}
