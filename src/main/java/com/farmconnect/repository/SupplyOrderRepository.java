package com.farmconnect.repository;

import com.farmconnect.model.SupplyOrder;
import com.farmconnect.model.SupplyOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface SupplyOrderRepository extends JpaRepository<SupplyOrder, Long> {
    List<SupplyOrder> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    List<SupplyOrder> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<SupplyOrder> findTop200ByOrderByCreatedAtDesc();
    List<SupplyOrder> findTop200ByStatusOrderByCreatedAtDesc(SupplyOrderStatus status);
    long countByStatus(SupplyOrderStatus status);

    @Query("select coalesce(sum(o.total), 0) from SupplyOrder o where o.status = com.farmconnect.model.SupplyOrderStatus.DELIVERED")
    java.math.BigDecimal deliveredGross();
    List<SupplyOrder> findByCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, SupplyOrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from SupplyOrder o where o.id = :id")
    Optional<SupplyOrder> lockById(@Param("id") Long id);
}
