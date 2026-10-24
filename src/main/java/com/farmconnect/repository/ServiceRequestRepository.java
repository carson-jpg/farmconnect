package com.farmconnect.repository;

import com.farmconnect.model.CompanyServiceRequest;
import com.farmconnect.model.ServiceRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServiceRequestRepository extends JpaRepository<CompanyServiceRequest, Long> {
    List<CompanyServiceRequest> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<CompanyServiceRequest> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);
    List<CompanyServiceRequest> findTop200ByOrderByCreatedAtDesc();
    List<CompanyServiceRequest> findTop200ByStatusOrderByCreatedAtDesc(ServiceRequestStatus status);
    long countByStatus(ServiceRequestStatus status);
    boolean existsByServiceIdAndFarmerIdAndStatusIn(Long serviceId, Long farmerId, Collection<ServiceRequestStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CompanyServiceRequest r where r.id = :id")
    Optional<CompanyServiceRequest> lockById(@Param("id") Long id);
}
