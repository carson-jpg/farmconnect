package com.farmconnect.repository;

import com.farmconnect.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
    List<AppNotification> findByUserIdAndReadAtIsNull(Long userId);
    long countByUserIdAndReadAtIsNull(Long userId);
}
