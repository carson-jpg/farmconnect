package com.farmconnect.repository;

import com.farmconnect.model.PurchaseOrder;
import com.farmconnect.model.PurchaseOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<PurchaseOrder> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);
    List<PurchaseOrder> findTop200ByOrderByCreatedAtDesc();
    List<PurchaseOrder> findTop200ByStatusOrderByCreatedAtDesc(PurchaseOrderStatus status);
    long countByStatus(PurchaseOrderStatus status);

    @Query("select coalesce(sum(p.quantity), 0) from PurchaseOrder p where p.request.id = :rid and p.status in :statuses")
    BigDecimal committedQuantity(@Param("rid") Long requestId, @Param("statuses") Collection<PurchaseOrderStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PurchaseOrder p where p.id = :id")
    Optional<PurchaseOrder> lockById(@Param("id") Long id);
}
