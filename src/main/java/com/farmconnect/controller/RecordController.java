package com.farmconnect.controller;

import com.farmconnect.dto.Community.*;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Digital farm records: planting, harvest, livestock, expenses, income and notes. Private to the farmer. */
@RestController
@RequestMapping("/api/records")
@PreAuthorize("hasRole('FARMER')")
@RequiredArgsConstructor
public class RecordController {
    private final FarmRecordRepository records;
    private final FarmRepository farms;
    private final OrderRepository orders;

    @GetMapping
    @Transactional(readOnly = true)
    public List<RecordResponse> list(@RequestParam(required = false) Long farmId, @RequestParam(required = false) RecordType type,
                                     @AuthenticationPrincipal User u) {
        return records.findByOwnerIdOrderByRecordDateDescIdDesc(u.getId()).stream()
                .filter(r -> farmId == null || r.getFarm().getId().equals(farmId))
                .filter(r -> type == null || r.getType() == type)
                .map(RecordController::toResponse).toList();
    }

    @PostMapping
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public RecordResponse create(@Valid @RequestBody RecordRequest r, @AuthenticationPrincipal User u) {
        FarmRecord fr = new FarmRecord();
        fr.setOwner(u);
        apply(fr, r, u);
        return toResponse(records.save(fr));
    }

    @PutMapping("/{id}")
    @Transactional
    public RecordResponse update(@PathVariable Long id, @Valid @RequestBody RecordRequest r, @AuthenticationPrincipal User u) {
        FarmRecord fr = owned(id, u);
        apply(fr, r, u);
        return toResponse(records.save(fr));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id, @AuthenticationPrincipal User u) {
        records.delete(owned(id, u));
    }

    /** Totals for the farmer (optionally one farm): money in / out, planted area, harvest per crop, herd sizes. */
    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public RecordSummary summary(@RequestParam(required = false) Long farmId, @AuthenticationPrincipal User u) {
        List<FarmRecord> list = records.findByOwnerIdOrderByRecordDateDescIdDesc(u.getId()).stream()
                .filter(r -> farmId == null || r.getFarm().getId().equals(farmId)).toList();
        BigDecimal income = BigDecimal.ZERO, expenses = BigDecimal.ZERO, acres = BigDecimal.ZERO;
        Map<String, BigDecimal> harvest = new LinkedHashMap<>();
        Map<String, BigDecimal> herd = new LinkedHashMap<>();      // newest count per animal (list is newest first)
        for (FarmRecord r : list) {
            BigDecimal amt = r.getAmount() == null ? BigDecimal.ZERO : r.getAmount();
            BigDecimal qty = r.getQuantity() == null ? BigDecimal.ZERO : r.getQuantity();
            switch (r.getType()) {
                case INCOME -> income = income.add(amt);
                case EXPENSE -> expenses = expenses.add(amt);
                case PLANTING -> acres = acres.add(qty);
                case HARVEST -> harvest.merge(r.getTitle(), qty, BigDecimal::add);
                case LIVESTOCK -> herd.putIfAbsent(r.getTitle(), qty);
                default -> { }
            }
        }
        // money earned through FarmConnect orders that were delivered
        BigDecimal platform = orders.findForFarmer(u.getId()).stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .flatMap(o -> o.getItems().stream())
                .filter(i -> i.getProduct().getFarm().getOwner().getId().equals(u.getId()))
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RecordSummary(income, expenses, income.add(platform).subtract(expenses), platform, acres, harvest, herd, list.size());
    }

    private void apply(FarmRecord fr, RecordRequest r, User u) {
        Farm farm = farms.findById(r.farmId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found"));
        if (!farm.getOwner().getId().equals(u.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your farm");
        if ((r.type() == RecordType.EXPENSE || r.type() == RecordType.INCOME) && (r.amount() == null || r.amount().signum() < 0))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter the amount in KES");
        if ((r.type() == RecordType.HARVEST || r.type() == RecordType.PLANTING || r.type() == RecordType.LIVESTOCK)
                && (r.quantity() == null || r.quantity().signum() < 0))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter the quantity");
        fr.setFarm(farm);
        fr.setType(r.type());
        fr.setTitle(r.title().trim());
        fr.setRecordDate(r.date() == null ? LocalDate.now() : r.date());
        fr.setQuantity(r.quantity());
        fr.setUnit(r.unit());
        fr.setAmount(r.amount());
        fr.setNotes(r.notes());
    }

    private FarmRecord owned(Long id, User u) {
        FarmRecord fr = records.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));
        if (!fr.getOwner().getId().equals(u.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your record");
        return fr;
    }

    private static RecordResponse toResponse(FarmRecord r) {
        return new RecordResponse(r.getId(), r.getFarm().getId(), r.getFarm().getName(), r.getType(), r.getTitle(), r.getRecordDate(),
                r.getQuantity(), r.getUnit(), r.getAmount(), r.getNotes());
    }
}
