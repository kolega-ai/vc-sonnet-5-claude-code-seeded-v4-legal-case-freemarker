package com.lawfirm.lcm.service;

import com.lawfirm.lcm.domain.Document;
import com.lawfirm.lcm.domain.DocumentVersion;
import com.lawfirm.lcm.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Assembles the submission copy the paralegal hands to the court: the latest filed
// version of the document is stamped with the paralegal's saved stamp profile and the
// result is kept alongside the document so it can be served from the matter file.
@Service
@RequiredArgsConstructor
public class CourtBundleService {

    private final FileStorageService fileStorageService;
    private final AuditService auditService;

    public Path assemble(Document doc, User actor, String stampProfile) throws IOException, InterruptedException {
        DocumentVersion version = doc.getVersions().stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Document has no versions"));
        Path src = fileStorageService.resolveForDownload(version.getStoredFilename());
        Path outDir = fileStorageService.filingDir().resolve("bundles");
        Files.createDirectories(outDir);
        Path out = outDir.resolve("bundle-" + doc.getId() + ".pdf");
        Path stamp = fileStorageService.filingDir().resolve("stamps").resolve(stampProfile + ".pdf");
        String cmd = "pdftk " + src + " multistamp " + stamp + " output " + out;
        Process proc = new ProcessBuilder("/bin/sh", "-c", cmd).start();
        proc.waitFor();
        auditService.log(actor, "COURT_BUNDLE_ASSEMBLE", "Document", doc.getId(), null, stampProfile);
        return out;
    }
}
