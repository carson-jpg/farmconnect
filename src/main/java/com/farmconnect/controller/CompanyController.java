package com.farmconnect.controller;

import com.farmconnect.dto.CompanyDtos.*;
import com.farmconnect.model.CompanyDocType;
import com.farmconnect.model.CompanyDocument;
import com.farmconnect.model.User;
import com.farmconnect.service.CompanyService;
import com.farmconnect.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** The company's own portal API (web and Android). */
@RestController
@RequestMapping("/api/company")
@PreAuthorize("hasRole('COMPANY')")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService service;
    private final FileStorageService files;

    /** 404 with message NO_COMPANY means "logged in, but the business details are not filled in yet". */
    @GetMapping("/me")
    public CompanyView me(@AuthenticationPrincipal User u) { return service.mine(u); }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyView register(@Valid @RequestBody CompanyRequest r, @AuthenticationPrincipal User u) {
        return service.register(u, r);
    }

    @PutMapping("/me")
    public CompanyView update(@Valid @RequestBody CompanyRequest r, @AuthenticationPrincipal User u) {
        return service.update(u, r);
    }

    @PostMapping(value = "/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompanyView upload(@RequestParam CompanyDocType type, @RequestParam("file") MultipartFile file,
                              @AuthenticationPrincipal User u) {
        return service.addDocument(u, type, file);
    }

    @DeleteMapping("/me/documents/{id}")
    public CompanyView deleteDocument(@PathVariable Long id, @AuthenticationPrincipal User u) {
        return service.deleteDocument(u, id);
    }

    @GetMapping("/me/documents/{id}")
    public ResponseEntity<byte[]> document(@PathVariable Long id, @AuthenticationPrincipal User u) {
        CompanyDocument d = service.myDocument(u, id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(FileStorageService.contentType(d.getPath())))
                .header("Cache-Control", "no-store").body(files.read(d.getPath()));
    }
}
