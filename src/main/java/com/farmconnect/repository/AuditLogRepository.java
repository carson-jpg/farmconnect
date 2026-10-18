package com.farmconnect.repository;

import com.farmconnect.model.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByIdDesc(Pageable page);
    long countByActionAndAtAfter(String action, Instant after);
    void deleteByAtBefore(Instant before);
}
