package com.farmconnect.controller;

import com.farmconnect.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/** Public product photos only. Verification documents are NEVER served from here. */
@RestController
@RequestMapping("/api/files/products")
@RequiredArgsConstructor
public class FileController {
    private final FileStorageService files;

    @GetMapping("/{productId}/{name:.+}")
    public ResponseEntity<byte[]> image(@PathVariable Long productId, @PathVariable String name) {
        if (!name.matches("[A-Za-z0-9\\-]+\\.(jpg|png)"))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "File not found");
        String rel = productId + "/" + name;
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(FileStorageService.contentType(rel)))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(files.readProductImage(rel));
    }
}
