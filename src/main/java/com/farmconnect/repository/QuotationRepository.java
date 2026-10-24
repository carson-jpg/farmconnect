package com.farmconnect.repository;

import com.farmconnect.model.Quotation;
import com.farmconnect.model.QuotationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Quotation> findByRequestIdOrderByPricePerUnitAscCreatedAtAsc(Long requestId);
    List<Quotation> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);
    long countByRequestIdAndStatus(Long requestId, QuotationStatus status);
    long countByStatus(QuotationStatus status);
    boolean existsByRequestIdAndFarmerIdAndStatusIn(Long requestId, Long farmerId, Collection<QuotationStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quotation q where q.id = :id")
    Optional<Quotation> lockById(@Param("id") Long id);
}
