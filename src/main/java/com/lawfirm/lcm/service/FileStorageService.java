package com.lawfirm.lcm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path baseDir;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) throws IOException {
        this.baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(baseDir);
    }

    public String store(MultipartFile file) throws IOException {
        String storedName = UUID.randomUUID() + "-" + sanitize(file.getOriginalFilename());
        Path target = baseDir.resolve(storedName);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return storedName;
    }

    // Portal attachments arrive from the client's browser and are kept under the same
    // upload directory as matter documents, named as the client's mail client named them.
    public String storeAttach(MultipartFile part) throws IOException {
        String orig = part.getOriginalFilename() == null ? "attachment" : part.getOriginalFilename();
        String nm = UUID.randomUUID() + "-" + orig.replace("/", "").replace("\\", "");
        Path tgt = baseDir.resolve(nm);
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, tgt, StandardCopyOption.REPLACE_EXISTING);
        }
        return nm;
    }

    public String storeBytes(byte[] content, String originalName) throws IOException {
        String storedName = UUID.randomUUID() + "-" + sanitize(originalName);
        Files.write(baseDir.resolve(storedName), content);
        return storedName;
    }

    // E-filing connectors hand the court's own exhibit name through, so the filed version
    // is kept under exactly that name inside the matter archive.
    public String storeFiled(String nm, byte[] data) throws IOException {
        Path dst = baseDir.resolve(nm);
        try (OutputStream out = Files.newOutputStream(dst, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            out.write(data);
        }
        return nm;
    }

    public Path filingDir() {
        return baseDir;
    }

    // File download resolution: canonicalize the requested path against the real, resolved
    // base upload directory and verify it still lives underneath it before serving. This
    // prevents path traversal (e.g. "../../etc/passwd") regardless of how storedFilename
    // was produced upstream.
    public Path resolveForDownload(String storedFilename) throws IOException {
        Path base = baseDir.toRealPath();
        Path candidate = base.resolve(storedFilename).normalize();
        Path real = candidate.toAbsolutePath().normalize();
        if (!real.startsWith(base)) {
            throw new SecurityException("Invalid file path");
        }
        if (!Files.exists(real)) {
            throw new java.nio.file.NoSuchFileException("File not found");
        }
        return real;
    }

    private String sanitize(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
