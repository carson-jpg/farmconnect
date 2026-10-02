package com.farmconnect.controller;

import com.farmconnect.dto.Dtos.*;
import com.farmconnect.dto.Mapper;
import com.farmconnect.model.*;
import com.farmconnect.repository.FarmRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/farms")
@RequiredArgsConstructor
public class FarmController {
    private final FarmRepository farms;

    @GetMapping
    public List<FarmResponse> all() {
        return farms.findAll().stream().map(Mapper::farm).toList();
    }

    @GetMapping("/{id}")
    public FarmResponse one(@PathVariable Long id) {
        return Mapper.farm(farms.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found")));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('FARMER')")
    public List<FarmResponse> mine(@AuthenticationPrincipal User u) {
        return farms.findByOwnerId(u.getId()).stream().map(Mapper::farm).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public FarmResponse create(@Valid @RequestBody FarmRequest r, @AuthenticationPrincipal User u) {
        Farm f = new Farm();
        f.setOwner(u);
        apply(f, r);
        return Mapper.farm(farms.save(f));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public FarmResponse update(@PathVariable Long id, @Valid @RequestBody FarmRequest r, @AuthenticationPrincipal User u) {
        Farm f = owned(id, u);
        apply(f, r);
        return Mapper.farm(farms.save(f));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal User u) {
        try {
            farms.delete(owned(id, u));
            farms.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delete this farm's products first");
        }
    }

    private void apply(Farm f, FarmRequest r) {
        f.setName(r.name());
        f.setLocation(r.location());
        f.setDescription(r.description());
    }

    private Farm owned(Long id, User u) {
        Farm f = farms.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Farm not found"));
        if (!f.getOwner().getId().equals(u.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your farm");
        return f;
    }
}
