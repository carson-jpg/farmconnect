package com.farmconnect.repository;

import com.farmconnect.model.CashSale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CashSaleRepository extends JpaRepository<CashSale, Long> {
    List<CashSale> findByOrderIdOrderByRecordedAtAsc(Long orderId);
    List<CashSale> findByFarmerIdOrderByRecordedAtDesc(Long farmerId);
    List<CashSale> findByOrderBuyerIdOrderByRecordedAtDesc(Long buyerId);
    List<CashSale> findByStatusInOrderByRecordedAtAsc(Collection<String> statuses);
    List<CashSale> findTop300ByOrderByRecordedAtDesc();
    boolean existsByOrderIdAndFarmerIdAndStatusIn(Long orderId, Long farmerId, Collection<String> statuses);
}
