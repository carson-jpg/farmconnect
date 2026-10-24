package com.farmconnect.repository;

import com.farmconnect.model.ProduceRequest;
import com.farmconnect.model.ProduceRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProduceRequestRepository extends JpaRepository<ProduceRequest, Long> {
    List<ProduceRequest> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<ProduceRequest> findTop200ByOrderByCreatedAtDesc();
    List<ProduceRequest> findTop200ByStatusOrderByCreatedAtDesc(ProduceRequestStatus status);
    long countByStatus(ProduceRequestStatus status);

    /** Open, not past its quotation deadline, and from a VERIFIED company. */
    @Query("select r from ProduceRequest r where r.status = com.farmconnect.model.ProduceRequestStatus.OPEN "
            + "and r.closesAt >= :today and r.company.status = com.farmconnect.model.CompanyStatus.VERIFIED order by r.createdAt desc")
    List<ProduceRequest> openFor(@Param("today") LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ProduceRequest r where r.id = :id")
    Optional<ProduceRequest> lockById(@Param("id") Long id);
}
