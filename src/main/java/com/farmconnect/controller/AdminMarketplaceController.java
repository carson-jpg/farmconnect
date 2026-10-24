package com.farmconnect.controller;

import com.farmconnect.dto.BulkDtos.PurchaseOrderView;
import com.farmconnect.dto.BulkDtos.RequestView;
import com.farmconnect.dto.MarketDtos.Overview;
import com.farmconnect.dto.MarketDtos.ServiceRequestView;
import com.farmconnect.dto.SupplyDtos.OrderView;
import com.farmconnect.model.ProduceRequestStatus;
import com.farmconnect.model.PurchaseOrderStatus;
import com.farmconnect.model.ServiceRequestStatus;
import com.farmconnect.model.SupplyOrderStatus;
import com.farmconnect.service.AdminMarketplaceService;
import com.farmconnect.service.BulkService;
import com.farmconnect.service.MarketplaceService;
import com.farmconnect.service.SupplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read-only oversight of everything companies do on the platform. SUPER ADMIN ONLY. */
@RestController
@RequestMapping("/api/admin/marketplace")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMarketplaceController {
    private final AdminMarketplaceService overview;
    private final SupplyService supply;
    private final BulkService bulk;
    private final MarketplaceService market;

    @GetMapping("/overview")
    public Overview overview() { return overview.overview(); }

    @GetMapping("/supply-orders")
    public List<OrderView> supplyOrders(@RequestParam(required = false) SupplyOrderStatus status) { return supply.adminOrders(status); }

    @GetMapping("/produce-requests")
    public List<RequestView> produceRequests(@RequestParam(required = false) ProduceRequestStatus status) { return bulk.adminRequests(status); }

    @GetMapping("/purchase-orders")
    public List<PurchaseOrderView> purchaseOrders(@RequestParam(required = false) PurchaseOrderStatus status) { return bulk.adminOrders(status); }

    @GetMapping("/service-requests")
    public List<ServiceRequestView> serviceRequests(@RequestParam(required = false) ServiceRequestStatus status) { return market.adminRequests(status); }
}
