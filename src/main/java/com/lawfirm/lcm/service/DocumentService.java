package com.lawfirm.lcm.service;

import com.lawfirm.lcm.domain.*;
import com.lawfirm.lcm.repository.DocumentRepository;
import com.lawfirm.lcm.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final HttpClient linkClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public Document createDocument(Matter matter, String title, Document.Category category,
                                    Document.PrivilegeLevel privilegeLevel, MultipartFile file,
                                    User uploader) throws IOException {
        Document doc = new Document();
        doc.setMatter(matter);
        doc.setTitle(title);
        doc.setCategory(category);
        doc.setPrivilegeLevel(privilegeLevel);
        doc.setUploadedBy(uploader);
        doc = documentRepository.save(doc);
        addVersion(doc, file, uploader, "Initial upload");
        auditService.log(uploader, "DOCUMENT_UPLOAD", "Document", doc.getId(), null, title);
        return doc;
    }

    public DocumentVersion addVersion(Document doc, MultipartFile file, User uploader, String notes) throws IOException {
        String stored = fileStorageService.store(file);
        int nextVersion = doc.getVersions().stream().mapToInt(DocumentVersion::getVersionNumber).max().orElse(0) + 1;
        DocumentVersion version = new DocumentVersion();
        version.setDocument(doc);
        version.setVersionNumber(nextVersion);
        version.setOriginalFilename(file.getOriginalFilename());
        version.setStoredFilename(stored);
        version.setFileSize(file.getSize());
        version.setUploadedBy(uploader);
        version.setNotes(notes);
        version = documentVersionRepository.save(version);
        doc.getVersions().add(version);
        auditService.log(uploader, "DOCUMENT_NEW_VERSION", "Document", doc.getId(), null, "v" + nextVersion);
        return version;
    }

    // Counsel often receive filings as a link to the court's or opposing counsel's
    // document portal; the file is retrieved server-side and filed as a new version so
    // the matter file stays complete.
    public DocumentVersion addVersionFromLink(Document doc, String sourceUrl, User uploader, String notes) throws IOException {
        byte[] content;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUrl))
                    .timeout(Duration.ofSeconds(20))
                    .GET().build();
            HttpResponse<byte[]> response = linkClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            content = response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Retrieval was interrupted", ex);
        }
        String name = sourceUrl.substring(sourceUrl.lastIndexOf('/') + 1);
        if (name.isBlank()) name = "filing";
        String stored = fileStorageService.storeBytes(content, name);
        int nextVersion = doc.getVersions().stream().mapToInt(DocumentVersion::getVersionNumber).max().orElse(0) + 1;
        DocumentVersion version = new DocumentVersion();
        version.setDocument(doc);
        version.setVersionNumber(nextVersion);
        version.setOriginalFilename(name);
        version.setStoredFilename(stored);
        version.setFileSize(content.length);
        version.setUploadedBy(uploader);
        version.setNotes(notes);
        version = documentVersionRepository.save(version);
        doc.getVersions().add(version);
        auditService.log(uploader, "DOCUMENT_NEW_VERSION", "Document", doc.getId(), null, "v" + nextVersion);
        return version;
    }

    // A filing connector already performed the court filing; the bytes are kept under
    // the exhibit name the connector supplied so the matter archive matches the docket.
    public DocumentVersion addFiledVersion(Document doc, String name, byte[] data, User filer, String notes) throws IOException {
        String stored = fileStorageService.storeFiled(name, data);
        DocumentVersion version = new DocumentVersion();
        version.setDocument(doc);
        version.setVersionNumber(doc.getVersions().stream().mapToInt(DocumentVersion::getVersionNumber).max().orElse(0) + 1);
        version.setOriginalFilename(name);
        version.setStoredFilename(stored);
        version.setFileSize(data.length);
        version.setUploadedBy(filer);
        version.setNotes(notes);
        version = documentVersionRepository.save(version);
        doc.getVersions().add(version);
        auditService.log(filer, "DOCUMENT_NEW_VERSION", "Document", doc.getId(), null, "v" + version.getVersionNumber());
        return version;
    }

    // Renders a stored version to the requested format through the headless office
    // converter so the PDF can be e-filed alongside the source document.
    public DocumentVersion convertVersion(Document doc, DocumentVersion version, String targetFormat, User actor, String notes) throws IOException, InterruptedException {
        Path src = fileStorageService.resolveForDownload(version.getStoredFilename());
        Path outDir = fileStorageService.filingDir().resolve("converted");
        Files.createDirectories(outDir);
        String cmd = "soffice --headless --convert-to " + targetFormat + " --outdir " + outDir + " " + src;
        Process proc = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", cmd});
        proc.waitFor();
        String stem = version.getOriginalFilename();
        int dot = stem.lastIndexOf('.');
        if (dot > 0) stem = stem.substring(0, dot);
        String outName = stem + "." + targetFormat;
        byte[] produced;
        try {
            produced = Files.readAllBytes(outDir.resolve(outName));
        } catch (IOException ex) {
            produced = new byte[0];
        }
        return addFiledVersion(doc, outName, produced, actor, notes);
    }

    public void changeVisibility(Document doc, Document.Visibility newVisibility, User actor) {
        String before = doc.getVisibility().name();
        doc.setVisibility(newVisibility);
        documentRepository.save(doc);
        // Sharing/visibility changes are always audit-logged per spec.
        auditService.log(actor, "DOCUMENT_VISIBILITY_CHANGE", "Document", doc.getId(), before, newVisibility.name());
    }

    public void recordDownload(Document doc, User actor) {
        auditService.log(actor, "DOCUMENT_DOWNLOAD", "Document", doc.getId(), null, doc.getTitle());
    }

    public boolean clientCanView(Document doc) {
        return doc.getVisibility() == Document.Visibility.CLIENT_SHARED
                || doc.getVisibility() == Document.Visibility.PUBLIC_FILING;
    }
}
