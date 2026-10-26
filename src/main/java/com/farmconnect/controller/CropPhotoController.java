package com.farmconnect.controller;

import com.farmconnect.service.CropPhotoSigner;
import com.farmconnect.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

/** Crop doctor photos. Open only with a signed link that the case API hands out for 6 hours. */
@RestController
@RequestMapping("/api/files/crop")
@RequiredArgsConstructor
public class CropPhotoController {
    private final FileStorageService files;
    private final CropPhotoSigner signer;

    @GetMapping("/{caseId}/{name:.+}")
    public ResponseEntity<byte[]> photo(@PathVariable Long caseId, @PathVariable String name,
                                        @RequestParam long e, @RequestParam String s) {
        if (!name.matches("[A-Za-z0-9\\-]+\\.(jpg|png)")) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        String rel = caseId + "/" + name;
        if (!signer.valid(rel, e, s)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(FileStorageService.contentType(rel)))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(files.readCropPhoto(rel));
    }
}
