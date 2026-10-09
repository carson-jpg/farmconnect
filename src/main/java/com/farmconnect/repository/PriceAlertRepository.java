package com.farmconnect.repository;

import com.farmconnect.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    List<PriceAlert> findByUserId(Long userId);
    Optional<PriceAlert> findByUserIdAndCropIgnoreCase(Long userId, String crop);
    List<PriceAlert> findByCropIgnoreCase(String crop);
}
