package com.farmconnect.repository;

import com.farmconnect.model.DeliveryJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DeliveryJobRepository extends JpaRepository<DeliveryJob, Long> {
    List<DeliveryJob> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    List<DeliveryJob> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);
    List<DeliveryJob> findByTransporterIdOrderByCreatedAtDesc(Long transporterId);
    List<DeliveryJob> findTop300ByOrderByCreatedAtDesc();
    List<DeliveryJob> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    boolean existsByOrderIdAndStatusIn(Long orderId, Collection<String> statuses);
    long countByTransporterIdAndStatus(Long transporterId, String status);
}
