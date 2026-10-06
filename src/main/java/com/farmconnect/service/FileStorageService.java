package com.farmconnect.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/** Stores verification photos on disk, outside any public web folder. Only JPG/PNG (checked by file signature). */
@Service
public class FileStorageService {
    private final Path root;

    public FileStorageService(@Value("${app.upload-dir:./uploads}") String dir) throws IOException {
        root = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public String save(Long ownerId, String prefix, MultipartFile f) {
        if (f == null || f.isEmpty()) throw bad("Empty file");
        if (f.getSize() > 6L * 1024 * 1024) throw bad("File too large (max 6 MB)");
        try {
            byte[] data = f.getBytes();
            String ext = ext(data);
            if (ext == null) throw bad("Only JPG or PNG images are allowed");
            Path dir = root.resolve("verification").resolve(String.valueOf(ownerId));
            Files.createDirectories(dir);
            String name = prefix + "-" + UUID.randomUUID() + ext;
            Files.write(dir.resolve(name), data);
            return "verification/" + ownerId + "/" + name;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file");
        }
    }

    public byte[] read(String relative) {
        try {
            Path p = root.resolve(relative).normalize();
            if (!p.startsWith(root) || !Files.exists(p)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read file");
        }
    }

    public void delete(String relative) {
        if (relative == null) return;
        try {
            Path p = root.resolve(relative).normalize();
            if (p.startsWith(root)) Files.deleteIfExists(p);
        } catch (IOException ignored) { }
    }

    public static String contentType(String relative) { return relative.endsWith(".png") ? "image/png" : "image/jpeg"; }

    private static String ext(byte[] d) {
        if (d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) return ".jpg";
        if (d.length > 4 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') return ".png";
        return null;
    }

    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}