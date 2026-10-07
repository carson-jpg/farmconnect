package com.farmconnect.controller;

import com.farmconnect.dto.Community.*;
import com.farmconnect.model.*;
import com.farmconnect.repository.ServiceProviderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Directory of agricultural service providers. Everyone reads; admins and officers maintain it. */
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {
    private final ServiceProviderRepository repo;

    @GetMapping
    public List<ServiceResponse> list(@RequestParam(required = false) ServiceCategory category, @RequestParam(required = false) String q,
                                      @RequestParam(required = false) String subCounty) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        return repo.findAllByOrderByNameAsc().stream()
                .filter(s -> category == null || s.getCategory() == category)
                .filter(s -> subCounty == null || subCounty.isBlank() || subCounty.equalsIgnoreCase(s.getSubCounty()))
                .filter(s -> needle.isEmpty() || s.getName().toLowerCase().contains(needle)
                        || (s.getServices() != null && s.getServices().toLowerCase().contains(needle)))
                .map(ServiceController::toResponse).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse create(@Valid @RequestBody ServiceRequest r) {
        ServiceProvider s = new ServiceProvider();
        apply(s, r);
        return toResponse(repo.save(s));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public ServiceResponse update(@PathVariable Long id, @Valid @RequestBody ServiceRequest r) {
        ServiceProvider s = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
        apply(s, r);
        return toResponse(repo.save(s));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        repo.deleteById(id);
    }

    private void apply(ServiceProvider s, ServiceRequest r) {
        s.setName(r.name().trim());
        s.setCategory(r.category());
        s.setDescription(r.description());
        s.setServices(r.services());
        s.setPhone(r.phone());
        s.setEmail(r.email());
        s.setSubCounty(r.subCounty());
        s.setLocation(r.location());
    }

    private static ServiceResponse toResponse(ServiceProvider s) {
        return new ServiceResponse(s.getId(), s.getName(), s.getCategory(), s.getDescription(), s.getServices(), s.getPhone(),
                s.getEmail(), s.getSubCounty(), s.getLocation());
    }
}
