package com.farmconnect.service;

import com.farmconnect.dto.MarketDtos.Overview;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Counts for the super admin's marketplace overview. */
@Service
@RequiredArgsConstructor
public class AdminMarketplaceService {
    private final CompanyRepository companies;
    private final SupplyOrderRepository supplyOrders;
    private final ProduceRequestRepository produceRequests;
    private final QuotationRepository quotations;
    private final PurchaseOrderRepository purchaseOrders;
    private final ServiceRequestRepository serviceRequests;

    @Transactional(readOnly = true)
    public Overview overview() {
        return new Overview(
                counts(CompanyStatus.values(), companies::countByStatus),
                counts(SupplyOrderStatus.values(), supplyOrders::countByStatus), supplyOrders.deliveredGross(),
                counts(ProduceRequestStatus.values(), produceRequests::countByStatus),
                counts(QuotationStatus.values(), quotations::countByStatus),
                counts(PurchaseOrderStatus.values(), purchaseOrders::countByStatus),
                counts(ServiceRequestStatus.values(), serviceRequests::countByStatus));
    }

    private static <E extends Enum<E>> Map<String, Long> counts(E[] values, Function<E, Long> count) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (E e : values) m.put(e.name(), count.apply(e));
        return m;
    }
}
