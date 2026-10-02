package com.farmconnect.repository;

import com.farmconnect.model.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    @Query("select distinct o from CustomerOrder o join o.items i where i.product.farm.owner.id = :farmerId order by o.createdAt desc")
    List<CustomerOrder> findForFarmer(@Param("farmerId") Long farmerId);
}
