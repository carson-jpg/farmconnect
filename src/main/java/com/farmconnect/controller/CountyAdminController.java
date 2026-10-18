package com.farmconnect.controller;

import com.farmconnect.dto.Dtos.UserSummary;
import com.farmconnect.dto.Mapper;
import com.farmconnect.model.Role;
import com.farmconnect.model.User;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.security.PasswordPolicy;
import com.farmconnect.service.Locations;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

/**
 * County administrators. ONLY the super administrator can create them, move them to another county or list them.
 * A county admin manages one county and nothing else; the county is chosen here, never by the county admin.
 */
@RestController
@RequestMapping("/api/admin/county-admins")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CountyAdminController {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public record CountyAdminRequest(@NotBlank String name, @NotBlank @Email String email, String phone,
                                     @NotBlank @Size(min = 8) String password, @NotBlank String county) {}
    public record CountyRequest(@NotBlank String county) {}

    @GetMapping
    @Transactional(readOnly = true)
    public List<UserSummary> list() {
        return users.findAll().stream()
                .filter(u -> u.getRole() == Role.COUNTY_ADMIN)
                .sorted(Comparator.comparing((User u) -> u.getAssignedCounty() == null ? "" : u.getAssignedCounty())
                        .thenComparing(u -> u.getName() == null ? "" : u.getName().toLowerCase()))
                .map(Mapper::user).toList();
    }

    @PostMapping
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary create(@Valid @RequestBody CountyAdminRequest r) {
        String county = r.county().trim();
        requireCounty(county);
        String email = r.email().trim();
        PasswordPolicy.require(r.password(), email);
        if (users.findByEmailIgnoreCase(email).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        User u = new User();
        u.setName(r.name().trim());
        u.setEmail(email);
        u.setPhone(r.phone() == null || r.phone().isBlank() ? null : r.phone().trim());
        u.setPassword(encoder.encode(r.password()));
        u.setRole(Role.COUNTY_ADMIN);
        u.setAssignedCounty(county);
        return Mapper.user(users.save(u));
    }

    /** Move a county admin to another county. */
    @PutMapping("/{id}/county")
    @Transactional
    public UserSummary assign(@PathVariable Long id, @Valid @RequestBody CountyRequest r) {
        String county = r.county().trim();
        requireCounty(county);
        User u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getRole() != Role.COUNTY_ADMIN)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only county admins can be assigned a county here");
        u.setAssignedCounty(county);
        u.getAssignedSubCounties().clear();
        return Mapper.user(users.save(u));
    }

    private static void requireCounty(String county) {
        if (!Locations.isCounty(county)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a county from the list");
    }
}
