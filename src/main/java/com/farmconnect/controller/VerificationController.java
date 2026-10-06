package com.farmconnect.controller;

import com.farmconnect.dto.Dtos.*;
import com.farmconnect.dto.Mapper;
import com.farmconnect.model.User;
import com.farmconnect.service.FileStorageService;
import com.farmconnect.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Farmer's own verification. A farmer can only ever see their own record. */
@RestController
@RequestMapping("/api/verification")
@PreAuthorize("hasRole('FARMER')")
@RequiredArgsConstructor
public class VerificationController {
    private final VerificationService service;
    private final FileStorageService files;

    @GetMapping("/me")
    public VerificationResponse me(@AuthenticationPrincipal User u) {
        return Mapper.verification(service.mine(u));
    }

    @PutMapping("/me")
    public VerificationResponse saveDraft(@RequestBody DraftRequest r, @AuthenticationPrincipal User u) {
        return Mapper.verification(service.saveDraft(u, r));
    }

    @PostMapping("/me/otp/send")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendOtp(@Valid @RequestBody PhoneRequest r, @AuthenticationPrincipal User u) {
        service.sendOtp(u, r.phone());
    }

    @PostMapping("/me/otp/verify")
    public VerificationResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest r, @AuthenticationPrincipal User u) {
        return Mapper.verification(service.verifyOtp(u, r.phone(), r.code()));
    }

    @PostMapping(value = "/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VerificationResponse upload(@RequestParam String type, @RequestParam("file") MultipartFile file,
                                       @AuthenticationPrincipal User u) {
        return Mapper.verification(service.upload(u, type, file));
    }

    @PostMapping("/me/submit")
    public VerificationResponse submit(@AuthenticationPrincipal User u) {
        return Mapper.verification(service.submit(u));
    }

    @GetMapping("/me/documents/{type}")
    public ResponseEntity<byte[]> document(@PathVariable String type, @AuthenticationPrincipal User u) {
        String path = VerificationService.pathFor(service.mine(u), type);
        if (path == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not uploaded");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(FileStorageService.contentType(path)))
                .header("Cache-Control", "no-store").body(files.read(path));
    }
}